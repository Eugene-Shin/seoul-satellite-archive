package com.all4land.seoulsatellitearchive.global.paging;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SliceRequest {

    @Schema(description = "첫 요청은 비워 둔다. 다음 페이지는 이전 응답의 nextCursor")
    private String cursor;

    @Min(1) @Max(100) private int size = 20;
}
