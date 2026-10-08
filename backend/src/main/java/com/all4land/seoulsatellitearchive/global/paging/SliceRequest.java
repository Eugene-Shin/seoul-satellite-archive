package com.all4land.seoulsatellitearchive.global.paging;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class SliceRequest {

    @Min(0) private int page = 0;

    @Min(1) @Max(100) private int size = 20;

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}
