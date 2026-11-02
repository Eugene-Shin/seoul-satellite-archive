package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import com.all4land.seoulsatellitearchive.image.dto.ImageDetailResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse.Range;
import com.all4land.seoulsatellitearchive.image.dto.ImageResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import com.all4land.seoulsatellitearchive.provider.Provider;
import com.all4land.seoulsatellitearchive.provider.ProviderRepository;
import com.all4land.seoulsatellitearchive.provider.dto.ProviderResponse;
import jakarta.persistence.EntityManager;
import java.util.List;
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
        return ImageDetailResponse.from(findImage(imageId));
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
        boolean duplicate = image.getId() == null
                ? satelliteImageRepository.existsByProvider_CodeAndSourceId(provider.getCode(), request.getSourceId())
                : satelliteImageRepository.existsByProvider_CodeAndSourceIdAndIdNot(
                        provider.getCode(), request.getSourceId(), image.getId());
        if (duplicate) {
            throw new BusinessException(CommonErrorCode.CONFLICT, "해당 제공 기관에 같은 sourceId의 영상이 존재합니다.");
        }
        image.replaceMetadata(request, provider, request.toFootprint());
        satelliteImageRepository.saveAndFlush(image);
        // @Formula는 쓰기 후 자동 갱신되지 않으므로 같은 영속성 컨텍스트에서 검색해도 최신 도형을 반환하게 한다.
        entityManager.refresh(image);
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
        return new SliceResponse<>(page.stream().map(ImageResponse::from).toList(), nextCursor);
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
