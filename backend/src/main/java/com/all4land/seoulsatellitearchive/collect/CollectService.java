package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.collect.Sentinel2Mapper.Mapped;
import com.all4land.seoulsatellitearchive.collect.dto.CollectRunResponse;
import com.all4land.seoulsatellitearchive.collection.ImageCollection;
import com.all4land.seoulsatellitearchive.collection.ImageCollectionRepository;
import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import com.all4land.seoulsatellitearchive.image.ImageFile;
import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.ImageFileRepository;
import com.all4land.seoulsatellitearchive.image.SatelliteImage;
import com.all4land.seoulsatellitearchive.image.SatelliteImageRepository;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectService {

    private final CollectTargetRepository collectTargetRepository;
    private final CollectRunRepository collectRunRepository;
    private final ImageCollectionRepository imageCollectionRepository;
    private final SatelliteImageRepository satelliteImageRepository;
    private final ImageFileRepository imageFileRepository;
    private final StacClient stacClient;
    private final Validator validator;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    private static final Duration HEARTBEAT_INTERVAL = Duration.ofMinutes(1);

    private enum SaveResult {
        INSERTED,
        UPDATED,
        UNCHANGED
    }

    // 검색에 필요한 값. 검색은 트랜잭션 밖에서 해서 응답을 기다리는 동안 DB 연결을 잡지 않는다
    private record RunContext(
            Long runId,
            Long targetId,
            Long collectionId,
            String searchUrl,
            String collectionCode,
            String aoiGeoJson,
            Instant windowFrom,
            Instant windowTo) {}

    // 실행 하나의 진행 상황. StacClient가 페이지마다 부르는 람다 안에서 바꾸므로 지역 변수 대신 객체에 담는다
    private static final class RunProgress {
        int found;
        int inserted;
        int updated;
        final List<CollectRunSkip> skips = new ArrayList<>();
        // 취소 요청을 보고 남은 영상을 처리하지 않았다
        boolean stopped;
    }

    public Long enqueue(Long targetId, CollectRunType runType, Instant windowFrom, Instant windowTo) {
        return enqueue(targetId, runType, windowFrom, windowTo, clock.instant());
    }

    // 기간은 단발 실행만 줄 수 있다. 없으면 워커가 시작할 때 정한다.
    // 스케줄러는 판단한 시각을 등록 시각으로 넘겨서 다음 cron 시각 계산과 맞춘다
    Long enqueue(Long targetId, CollectRunType runType, Instant windowFrom, Instant windowTo, Instant createdAt) {
        if (runType == CollectRunType.SCHEDULED && windowFrom != null) {
            throw new IllegalArgumentException("정기 실행은 조회 구간을 지정하지 않습니다.");
        }
        if ((windowFrom == null) != (windowTo == null) || (windowFrom != null && !windowFrom.isBefore(windowTo))) {
            throw new BusinessException(
                    CommonErrorCode.BAD_REQUEST, "windowFrom과 windowTo는 함께 주고, windowFrom이 windowTo보다 앞서야 합니다.");
        }
        return transactionTemplate.execute(status -> {
            CollectTarget target = collectTargetRepository
                    .findById(targetId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
            String collectionCode = target.getCollection().getCode();
            if (!Sentinel2Mapper.COLLECTION.equals(collectionCode)) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "수집할 수 없는 컬렉션입니다: " + collectionCode);
            }
            try {
                return collectRunRepository
                        .saveAndFlush(new CollectRun(target, runType, windowFrom, windowTo, createdAt))
                        .getId();
            } catch (DataIntegrityViolationException e) {
                throw new BusinessException(CommonErrorCode.CONFLICT, "이미 대기 중이거나 실행 중인 수집입니다.");
            }
        });
    }

    // 대기 중이면 바로 취소한다. 실행 중이면 요청만 남기고, 상태는 워커가 확인해서 바꾼다
    public void requestCancel(Long runId) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant now = clock.instant();
            if (collectRunRepository.cancelQueued(runId, now) == 1
                    || collectRunRepository.requestCancelRunning(runId, now) == 1) {
                return;
            }
            CollectRun run = collectRunRepository
                    .findById(runId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
            if (run.getStatus().isFinished()) {
                throw new BusinessException(CommonErrorCode.CONFLICT, "이미 끝난 실행입니다.");
            }
        });
    }

    public CollectRunResponse getRun(Long runId) {
        return transactionTemplate.execute(status -> collectRunRepository
                .findById(runId)
                .map(CollectRunResponse::from)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND)));
    }

    // 대기 중인 실행 하나를 가져가 끝까지 처리한다. 대기열이 비었으면 false
    public boolean processNext() {
        RunContext context = transactionTemplate.execute(status -> claimNext());
        if (context == null) {
            return false;
        }
        execute(context);
        return true;
    }

    private RunContext claimNext() {
        CollectRun run = collectRunRepository.lockNextQueued().orElse(null);
        if (run == null) {
            return null;
        }
        CollectTarget target = run.getTarget();
        ImageCollection collection = target.getCollection();
        Instant now = clock.instant();
        Instant from = run.getWindowFrom();
        Instant to = run.getWindowTo();
        if (from == null) {
            // 실패한 기간을 다음 실행이 다시 조회하도록 마지막으로 성공한 정기 실행에서 이어 간다
            from = collectRunRepository
                    .findLastScheduledSuccess(target.getId())
                    .map(CollectRun::getWindowTo)
                    .orElse(now)
                    .minus(target.getLookback());
            to = now;
        }
        run.start(from, to, now);
        return new RunContext(
                run.getId(),
                target.getId(),
                collection.getId(),
                collection.getProvider().getMetadataSearchUrl(),
                collection.getCode(),
                target.getAoiGeoJson(),
                from,
                to);
    }

    // 실행 기록과 영상은 각각 따로 커밋한다. 영상 하나가 실패해도 나머지는 저장되고, 실행이 실패해도 기록이 남는다.
    // 취소 요청은 영상마다, 다음 페이지를 받기 전마다 확인한다. 확인하기 전에 끝까지 처리했으면 성공으로 끝낸다
    private void execute(RunContext context) {
        Long runId = context.runId();
        RunProgress progress = new RunProgress();
        try {
            boolean reachedEnd = stacClient.search(
                    context.searchUrl(),
                    context.collectionCode(),
                    JsonMapper.shared().readTree(context.aoiGeoJson()),
                    context.windowFrom(),
                    context.windowTo(),
                    items -> {
                        progress.found += items.size();
                        for (JsonNode item : items) {
                            if (checkpoint(runId)) {
                                progress.stopped = true;
                                return false;
                            }
                            saveCounting(context, item, progress);
                        }
                        return !checkpoint(runId);
                    });
            boolean canceled = progress.stopped || !reachedEnd;
            transactionTemplate.executeWithoutResult(status -> finish(findRun(runId), canceled, progress));
        } catch (RuntimeException e) {
            log.warn("수집 실패 runId={}", runId, e);
            transactionTemplate.executeWithoutResult(status -> findRun(runId).fail(reason(e), clock.instant()));
        }
    }

    // 하트비트를 남기고 취소 요청이 있는지 돌려준다
    private boolean checkpoint(Long runId) {
        Instant now = clock.instant();
        return collectRunRepository.recordHeartbeatAndCheckCancel(runId, now, now.minus(HEARTBEAT_INTERVAL));
    }

    private void saveCounting(RunContext context, JsonNode item, RunProgress progress) {
        try {
            switch (transactionTemplate.execute(status -> save(context, item))) {
                case INSERTED -> progress.inserted++;
                case UPDATED -> progress.updated++;
                case UNCHANGED -> {}
            }
        } catch (RuntimeException e) {
            String sourceId = item.path("id").asString("(id 없음)");
            log.warn("영상을 건너뜁니다 runId={} sourceId={}", context.runId(), sourceId, e);
            progress.skips.add(new CollectRunSkip(sourceId, reason(e)));
        }
    }

    private void finish(CollectRun run, boolean canceled, RunProgress progress) {
        if (canceled) {
            run.cancel(progress.found, progress.inserted, progress.updated, progress.skips, clock.instant());
        } else {
            run.succeed(progress.found, progress.inserted, progress.updated, progress.skips, clock.instant());
        }
    }

    private void validate(ImageWriteRequest request) {
        Set<ConstraintViolation<ImageWriteRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
    }

    // 출처 갱신 일시가 그대로면 바뀐 것이 없다고 보고 건드리지 않는다
    private SaveResult save(RunContext context, JsonNode item) {
        Mapped mapped = Sentinel2Mapper.map(item, context.collectionId());
        ImageWriteRequest request = mapped.request();
        validate(request);
        MultiPolygon footprint = request.toFootprint();
        Optional<SatelliteImage> existing =
                satelliteImageRepository.findByCollection_IdAndSourceId(context.collectionId(), request.getSourceId());
        Instant sourceUpdatedAt = request.getSourceUpdatedAt() == null
                ? null
                : request.getSourceUpdatedAt().toInstant();
        if (existing.isPresent() && Objects.equals(existing.get().getSourceUpdatedAt(), sourceUpdatedAt)) {
            return SaveResult.UNCHANGED;
        }
        SatelliteImage image = existing.orElseGet(
                () -> new SatelliteImage(collectTargetRepository.getReferenceById(context.targetId())));
        image.replaceMetadata(request, imageCollectionRepository.getReferenceById(context.collectionId()), footprint);
        satelliteImageRepository.saveAndFlush(image);
        syncFiles(image, mapped.files());
        return existing.isPresent() ? SaveResult.UPDATED : SaveResult.INSERTED;
    }

    // 같은 링크의 행은 값만 바꾸고(아카이빙 정보 보존), 빠진 링크는 지우고, 새 링크는 추가한다
    private void syncFiles(SatelliteImage image, Map<String, ImageFileDetails> files) {
        Map<String, ImageFileDetails> added = new LinkedHashMap<>(files);
        List<ImageFile> removed = new ArrayList<>();
        for (ImageFile file : imageFileRepository.findAllByImage_Id(image.getId())) {
            ImageFileDetails details = added.remove(file.getHref());
            if (details == null) {
                removed.add(file);
            } else {
                file.replaceDetails(details);
            }
        }
        imageFileRepository.deleteAll(removed);
        imageFileRepository.saveAll(added.entrySet().stream()
                .map(entry -> new ImageFile(image, entry.getKey(), entry.getValue()))
                .toList());
        imageFileRepository.flush();
    }

    private CollectRun findRun(Long runId) {
        return collectRunRepository.findById(runId).orElseThrow();
    }

    private static String reason(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
