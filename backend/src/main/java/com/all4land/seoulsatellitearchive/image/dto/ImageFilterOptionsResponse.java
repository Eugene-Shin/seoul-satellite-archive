package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.image.OrbitDirection;
import com.all4land.seoulsatellitearchive.image.SensorType;
import com.all4land.seoulsatellitearchive.provider.dto.ProviderResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ImageFilterOptionsResponse(
        List<String> satellites,
        List<String> instruments,
        List<ProviderResponse> providers,
        List<SensorType> sensorTypes,
        List<OrbitDirection> orbitDirections,
        Range<BigDecimal> gsdRange,
        Range<Instant> acquiredRange) {

    public record Range<T>(T min, T max) {}
}
