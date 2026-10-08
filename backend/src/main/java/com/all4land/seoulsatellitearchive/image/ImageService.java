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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private final SatelliteImageRepository satelliteImageRepository;
    private final ProviderRepository providerRepository;

    public SliceResponse<ImageResponse> search(ImageSearchRequest request) {
        return SliceResponse.from(satelliteImageRepository
                .findBy(ImageSearchSpecification.of(request), query -> query.slice(request.toPageable()))
                .map(ImageResponse::from));
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
