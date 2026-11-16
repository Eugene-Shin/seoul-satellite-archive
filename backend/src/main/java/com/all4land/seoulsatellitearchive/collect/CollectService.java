package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.collect.Sentinel2Mapper.Mapped;
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

    // 실행 기록과 영상은 각각 따로 커밋한다. 영상 하나가 실패해도 나머지는 저장되고, 실행이 실패해도 기록이 남는다
    public CollectRun run(Long targetId) {
        RunContext context = transactionTemplate.execute(status -> start(targetId));
        Long runId = context.runId();
        try {
            List<JsonNode> items = stacClient.search(
                    context.searchUrl(),
                    context.collectionCode(),
                    JsonMapper.shared().readTree(context.aoiGeoJson()),
                    context.windowFrom(),
                    context.windowTo());
            int inserted = 0;
            int updated = 0;
            List<CollectRunSkip> skips = new ArrayList<>();
            for (JsonNode item : items) {
                try {
                    switch (transactionTemplate.execute(status -> save(context, item))) {
                        case INSERTED -> inserted++;
                        case UPDATED -> updated++;
                        case UNCHANGED -> {}
                    }
                } catch (RuntimeException e) {
                    String sourceId = item.path("id").asString("(id 없음)");
                    log.warn("영상을 건너뜁니다 runId={} sourceId={}", runId, sourceId, e);
                    skips.add(new CollectRunSkip(sourceId, reason(e)));
                }
            }
            int found = items.size();
            int insertedCount = inserted;
            int updatedCount = updated;
            transactionTemplate.executeWithoutResult(
                    status -> findRun(runId).succeed(found, insertedCount, updatedCount, skips, clock.instant()));
        } catch (RuntimeException e) {
            log.warn("수집 실패 runId={}", runId, e);
            transactionTemplate.executeWithoutResult(status -> findRun(runId).fail(reason(e), clock.instant()));
        }
        return findRun(runId);
    }

    // 실패한 기간을 다음 실행이 다시 조회하도록 마지막 성공 실행에서 이어 간다
    private RunContext start(Long targetId) {
        CollectTarget target = collectTargetRepository
                .findById(targetId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
        ImageCollection collection = target.getCollection();
        if (!Sentinel2Mapper.COLLECTION.equals(collection.getCode())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "수집할 수 없는 컬렉션입니다: " + collection.getCode());
        }
        Instant now = clock.instant();
        Instant from = collectRunRepository
                .findLastSucceeded(targetId)
                .map(CollectRun::getWindowTo)
                .orElse(now)
                .minus(target.getLookback());
        CollectRun run;
        try {
            run = collectRunRepository.saveAndFlush(new CollectRun(target, from, now, now));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.CONFLICT, "이미 실행 중인 수집입니다.");
        }
        return new RunContext(
                run.getId(),
                targetId,
                collection.getId(),
                collection.getProvider().getMetadataSearchUrl(),
                collection.getCode(),
                target.getAoiGeoJson(),
                from,
                now);
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
