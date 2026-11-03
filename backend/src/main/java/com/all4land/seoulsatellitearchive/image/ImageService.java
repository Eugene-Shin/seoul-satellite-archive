package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.collection.ImageCollection;
import com.all4land.seoulsatellitearchive.collection.ImageCollectionRepository;
import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import com.all4land.seoulsatellitearchive.image.dto.ImageDetailResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse.Range;
import com.all4land.seoulsatellitearchive.image.dto.ImageResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest.SarRequest;
import com.all4land.seoulsatellitearchive.provider.Provider;
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
    private final EntityManager entityManager;

    // 직접 등록하는 영상은 우리 기관이 보유한 것이다. API 출처의 영상은 수집기만 만든다
    @Transactional
    public void create(ImageWriteRequest request) {
        Provider ownOrganization = providerRepository
                .findById(Provider.OWN_ORGANIZATION_CODE)
                .orElseThrow(() -> new IllegalStateException("우리 기관 제공 기관 행이 없습니다."));
        replaceMetadata(new SatelliteImage(), ownOrganization, request);
    }

    public ImageDetailResponse getDetail(Long imageId) {
        SatelliteImage image = findImage(imageId);
        return ImageDetailResponse.from(
                image, imageSarRepository.findById(imageId).orElse(null));
    }

    @Transactional
    public void update(Long imageId, ImageWriteRequest request) {
        SatelliteImage image = findImage(imageId);
        replaceMetadata(image, image.getProvider(), request);
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

    private void replaceMetadata(SatelliteImage image, Provider provider, ImageWriteRequest request) {
        ImageCollection collection = imageCollectionRepository
                .findById(request.getCollectionId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST, "collectionId: 존재하지 않는 컬렉션입니다."));
        // 올포랜드는 보유한 어느 출처의 제품이든 올린다. 다른 출처의 영상은 그 출처의 컬렉션만 가진다
        if (!provider.isOwnOrganization() && !collection.getProvider().getCode().equals(provider.getCode())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "collectionId: 이 제공 기관의 컬렉션이 아닙니다.");
        }
        boolean duplicate = image.getId() == null
                ? satelliteImageRepository.existsByProvider_CodeAndSourceId(provider.getCode(), request.getSourceId())
                : satelliteImageRepository.existsByProvider_CodeAndSourceIdAndIdNot(
                        provider.getCode(), request.getSourceId(), image.getId());
        if (duplicate) {
            throw new BusinessException(CommonErrorCode.CONFLICT, "해당 제공 기관에 같은 sourceId의 영상이 존재합니다.");
        }
        image.replaceMetadata(request, provider, collection, request.toFootprint());
        satelliteImageRepository.saveAndFlush(image);
        replaceSar(image, request.getSar());
        if (provider.isOwnOrganization()) {
            syncFiles(image, request.getDownloadUrls());
        }
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
        // 이 페이지 영상들의 SAR 정보를 IN 쿼리 한 번으로 가져온다 (영상마다 조회하면 N+1)
        Map<Long, ImageSar> sars =
                imageSarRepository
                        .findAllById(page.stream().map(SatelliteImage::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(ImageSar::getImageId, Function.identity()));
        return new SliceResponse<>(
                page.stream()
                        .map(image -> ImageResponse.from(image, sars.get(image.getId())))
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
                new Range<>(ranges.getGsdMin(), ranges.getGsdMax()),
                new Range<>(ranges.getAcquiredMin(), ranges.getAcquiredMax()));
    }
}
