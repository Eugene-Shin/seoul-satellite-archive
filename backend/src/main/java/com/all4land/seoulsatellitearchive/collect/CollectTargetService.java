package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.collect.dto.AoiPresetResponse;
import com.all4land.seoulsatellitearchive.collect.dto.CollectTargetRequest;
import com.all4land.seoulsatellitearchive.collect.dto.CollectTargetResponse;
import com.all4land.seoulsatellitearchive.collection.ImageCollection;
import com.all4land.seoulsatellitearchive.collection.ImageCollectionRepository;
import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.geo.GeoJsonMultiPolygon;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectTargetService {

    private final CollectTargetRepository collectTargetRepository;
    private final CollectRunRepository collectRunRepository;
    private final ImageCollectionRepository imageCollectionRepository;
    private final AoiPresetRepository aoiPresetRepository;
    private final EntityManager entityManager;

    public List<CollectTargetResponse> findAll() {
        return collectTargetRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    // 새 수집 대상의 영역 기본값
    public AoiPresetResponse getDefaultAoi() {
        return aoiPresetRepository
                .findById(AoiPreset.SEOUL)
                .map(AoiPresetResponse::from)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    }

    @Transactional
    public CollectTargetResponse create(CollectTargetRequest request) {
        ImageCollection collection = imageCollectionRepository
                .findById(request.collectionId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST, "collectionId: 존재하지 않는 컬렉션입니다."));
        if (!CollectableCollections.contains(collection.getCode())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "collectionId: 수집할 수 없는 컬렉션입니다.");
        }
        // 형식 오류(400)를 먼저 알리고 중복(409)은 그 뒤에 본다
        MultiPolygon aoi = aoi(request);
        String scheduleCron = scheduleCron(request);
        if (collectTargetRepository.existsForCollection(collection.getId())) {
            throw duplicateCollection();
        }
        CollectTarget target = new CollectTarget(
                collection, aoi, scheduleCron, Duration.ofDays(request.lookbackDays()), request.enabled());
        // 위 검사와 저장 사이에 다른 요청이 먼저 등록하면 유니크 제약이 막는다
        try {
            return save(target);
        } catch (DataIntegrityViolationException e) {
            throw duplicateCollection();
        }
    }

    private static BusinessException duplicateCollection() {
        return new BusinessException(
                CommonErrorCode.CONFLICT, "collectionId: 이 컬렉션의 수집 대상이 이미 있습니다. 기존 대상의 영역에 지역을 추가하세요.");
    }

    @Transactional
    public CollectTargetResponse update(Long targetId, CollectTargetRequest request) {
        CollectTarget target = findTarget(targetId);
        if (!target.getCollection().getId().equals(request.collectionId())) {
            throw new BusinessException(
                    CommonErrorCode.BAD_REQUEST, "collectionId: 수집 대상의 컬렉션은 바꿀 수 없습니다. 새 수집 대상을 만드세요.");
        }
        target.replaceSettings(
                aoi(request), scheduleCron(request), Duration.ofDays(request.lookbackDays()), request.enabled());
        return save(target);
    }

    // 실행 이력은 함께 지워지고, 수집한 영상은 남아서 수집 영상이 아니게 된다(수정할 수 있게 됨)
    @Transactional
    public void delete(Long targetId) {
        CollectTarget target = findTarget(targetId);
        if (collectRunRepository.existsActive(targetId)) {
            throw new BusinessException(CommonErrorCode.CONFLICT, "대기 중이거나 실행 중인 수집이 있어 삭제할 수 없습니다.");
        }
        collectTargetRepository.delete(target);
        collectTargetRepository.flush();
    }

    private CollectTargetResponse save(CollectTarget target) {
        collectTargetRepository.saveAndFlush(target);
        // @Formula(aoiGeoJson)는 쓰기 후 자동으로 갱신되지 않는다
        entityManager.refresh(target);
        return toResponse(target);
    }

    private CollectTargetResponse toResponse(CollectTarget target) {
        return CollectTargetResponse.from(
                target,
                collectRunRepository.findLastScheduledCreatedAt(target.getId()).orElse(null));
    }

    private CollectTarget findTarget(Long targetId) {
        return collectTargetRepository
                .findById(targetId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    }

    private static MultiPolygon aoi(CollectTargetRequest request) {
        return GeoJsonMultiPolygon.parse(request.aoi(), "aoi");
    }

    private static String scheduleCron(CollectTargetRequest request) {
        String cron = request.scheduleCron().strip();
        if (!CronExpression.isValidExpression(cron)) {
            throw new BusinessException(
                    CommonErrorCode.BAD_REQUEST, "scheduleCron: Spring cron 형식(초 분 시 일 월 요일)이어야 합니다.");
        }
        return cron;
    }
}
