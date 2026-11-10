package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.collection.ImageCollection;
import com.all4land.seoulsatellitearchive.collection.ImageCollectionRepository;
import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import com.all4land.seoulsatellitearchive.image.ImageFileRepository.ImageFileUrl;
import com.all4land.seoulsatellitearchive.image.dto.ImageDetailResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse.Range;
import com.all4land.seoulsatellitearchive.image.dto.ImageResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest.SarRequest;
import com.all4land.seoulsatellitearchive.provider.ProviderRepository;
import com.all4land.seoulsatellitearchive.provider.dto.ProviderResponse;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private final SatelliteImageRepository satelliteImageRepository;
    private final ProviderRepository providerRepository;
    private final ImageCollectionRepository imageCollectionRepository;
    private final ImageSarRepository imageSarRepository;
    private final ImageFileRepository imageFileRepository;
    private final ImageResolutionRepository imageResolutionRepository;
    private final EntityManager entityManager;

    @Transactional
    public void create(ImageWriteRequest request) {
        replaceMetadata(new SatelliteImage(), request);
    }

    public ImageDetailResponse getDetail(Long imageId) {
        SatelliteImage image = findImage(imageId);
        return ImageDetailResponse.from(
                image,
                imageSarRepository.findById(imageId).orElse(null),
                imageResolutionRepository.findById(imageId).orElse(null),
                imageFileRepository.findAllWithBandsByImage_IdOrderByRoleAscAssetKeyAsc(imageId));
    }

    @Transactional
    public void update(Long imageId, ImageWriteRequest request) {
        replaceMetadata(findImage(imageId), request);
    }

    @Transactional
    public void delete(Long imageId) {
        satelliteImageRepository.delete(findImage(imageId));
        satelliteImageRepository.flush();
    }

    private SatelliteImage findImage(Long imageId) {
        return satelliteImageRepository
                .findById(imageId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    }

    private void replaceMetadata(SatelliteImage image, ImageWriteRequest request) {
        ImageCollection collection = imageCollectionRepository
                .findById(request.getCollectionId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST, "collectionId: 존재하지 않는 컬렉션입니다."));
        boolean duplicate = image.getId() == null
                ? satelliteImageRepository.existsByCollection_IdAndSourceId(collection.getId(), request.getSourceId())
                : satelliteImageRepository.existsByCollection_IdAndSourceIdAndIdNot(
                        collection.getId(), request.getSourceId(), image.getId());
        if (duplicate) {
            throw new BusinessException(CommonErrorCode.CONFLICT, "해당 컬렉션에 같은 sourceId의 영상이 존재합니다.");
        }
        image.replaceMetadata(request, collection, request.toFootprint());
        satelliteImageRepository.saveAndFlush(image);
        replaceSar(image, request.getSar());
        syncFiles(image, request.getDownloadUrls());
        // @Formula는 쓰기 후 자동 갱신되지 않으므로 같은 영속성 컨텍스트에서 검색해도 최신 도형을 반환하게 한다.
        entityManager.refresh(image);
    }

    // 수정은 전체 교체라서 sar가 없으면 기존 행을 지운다
    private void replaceSar(SatelliteImage image, SarRequest request) {
        ImageSar sar = imageSarRepository.findById(image.getId()).orElse(null);
        if (request == null) {
            if (sar != null) {
                imageSarRepository.delete(sar);
                imageSarRepository.flush();
            }
            return;
        }
        if (sar == null) {
            sar = new ImageSar(image);
        }
        sar.replace(request);
        imageSarRepository.saveAndFlush(sar);
    }

    // 그대로인 URL의 행은 남기고(나중에 채울 아카이빙 정보 보존), 빠진 URL은 지우고, 새 URL은 추가한다
    private void syncFiles(SatelliteImage image, List<String> downloadUrls) {
        Set<String> added = new LinkedHashSet<>(downloadUrls);
        List<ImageFile> removed = new ArrayList<>();
        for (ImageFile file : imageFileRepository.findAllByImage_Id(image.getId())) {
            if (!added.remove(file.getHref())) {
                removed.add(file);
            }
        }
        imageFileRepository.deleteAll(removed);
        imageFileRepository.saveAll(
                added.stream().map(href -> new ImageFile(image, href)).toList());
        imageFileRepository.flush();
    }

    public SliceResponse<ImageResponse> search(ImageSearchRequest request) {
        ImageCursor cursor = StringUtils.hasText(request.getCursor())
                ? ImageCursor.decode(request.getCursor(), request.getSorts())
                : null;
        int size = request.getSize();
        // 한 건 더 가져와서 다음 페이지가 있는지 판단한다
        List<SatelliteImage> rows = satelliteImageRepository.findBy(
                ImageSearchSpecification.of(request).and(ImageSearchSpecification.after(cursor)),
                query -> query.sortBy(request.toSort()).limit(size + 1).all());
        List<SatelliteImage> page = rows.size() > size ? rows.subList(0, size) : rows;
        String nextCursor = rows.size() > size
                ? ImageCursor.of(page.getLast(), request.getSorts()).encode()
                : null;
        // 이 페이지 영상들의 SAR 정보·해상도·썸네일을 IN 쿼리 한 번씩으로 가져온다 (영상마다 조회하면 N+1)
        List<Long> imageIds = page.stream().map(SatelliteImage::getId).toList();
        Map<Long, ImageSar> sars = imageSarRepository.findAllById(imageIds).stream()
                .collect(Collectors.toMap(ImageSar::getImageId, Function.identity()));
        Map<Long, ImageResolution> resolutions = imageResolutionRepository.findAllById(imageIds).stream()
                .collect(Collectors.toMap(ImageResolution::getImageId, Function.identity()));
        Map<Long, String> thumbnailUrls = imageFileRepository.findThumbnails(imageIds).stream()
                .collect(Collectors.toMap(ImageFileUrl::getImageId, ImageFileUrl::getHref));
        Map<Long, String> overlayUrls = imageFileRepository.findOverlays(imageIds).stream()
                .collect(Collectors.toMap(ImageFileUrl::getImageId, ImageFileUrl::getHref));
        return new SliceResponse<>(
                page.stream()
                        .map(image -> ImageResponse.from(
                                image,
                                sars.get(image.getId()),
                                resolutions.get(image.getId()),
                                thumbnailUrls.get(image.getId()),
                                overlayUrls.get(image.getId())))
                        .toList(),
                nextCursor);
    }

    public ImageFilterOptionsResponse getFilterOptions() {
        SatelliteImageRepository.Ranges ranges = satelliteImageRepository.findRanges();
        return new ImageFilterOptionsResponse(
                satelliteImageRepository.findDistinctSatellites(),
                satelliteImageRepository.findDistinctInstruments(),
                providerRepository.findAll(Sort.by("code")).stream()
                        .map(ProviderResponse::from)
                        .toList(),
                List.of(SensorType.values()),
                List.of(OrbitDirection.values()),
                new Range<>(ranges.getResolutionMin(), ranges.getResolutionMax()),
                new Range<>(ranges.getAcquiredMin(), ranges.getAcquiredMax()));
    }
}
