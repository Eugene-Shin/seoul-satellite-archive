package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.global.paging.SliceRequest;
import com.all4land.seoulsatellitearchive.image.ImageSort;
import com.all4land.seoulsatellitearchive.image.OrbitDirection;
import com.all4land.seoulsatellitearchive.image.SensorType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ImageSearchRequest extends SliceRequest {

    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate acquiredFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate acquiredTo;

    private List<String> satellites;

    private List<String> instruments;

    private List<SensorType> sensorTypes;

    private OrbitDirection orbitDirection;

    private List<String> providers;

    private Boolean isPaid;

    @DecimalMin("0") private BigDecimal gsdMin;

    @DecimalMin("0") private BigDecimal gsdMax;

    @DecimalMin("0") @DecimalMax("100") private BigDecimal cloudMax;

    @Size(min = 4, max = 4, message = "minLon,minLat,maxLon,maxLat 4개 값이어야 합니다.") private List<@NotNull Double> bbox;

    private List<@NotNull ImageSort> sorts = List.of(ImageSort.ACQUIRED_AT_DESC);

    public Pageable toPageable() {
        List<Sort.Order> orders =
                new ArrayList<>(sorts.stream().map(ImageSort::toOrder).toList());
        orders.add(Sort.Order.desc("id"));
        return toPageable(Sort.by(orders));
    }
}
