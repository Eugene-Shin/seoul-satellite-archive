package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse.Range;
import com.all4land.seoulsatellitearchive.image.dto.ImageResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import com.all4land.seoulsatellitearchive.provider.ProviderRepository;
import com.all4land.seoulsatellitearchive.provider.dto.ProviderResponse;
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
