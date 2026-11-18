package com.all4land.seoulsatellitearchive.collect.dto;

import com.all4land.seoulsatellitearchive.global.paging.SliceRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectRunSearchRequest extends SliceRequest {

    @Schema(description = "선택. 이 수집 대상의 실행만. 없으면 모든 대상")
    private Long targetId;
}
