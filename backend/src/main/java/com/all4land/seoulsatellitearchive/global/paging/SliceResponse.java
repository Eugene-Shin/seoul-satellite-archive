package com.all4land.seoulsatellitearchive.global.paging;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SliceResponse<T>(
        List<T> content,
        @Schema(description = "다음 요청의 cursor. 더 없으면 null") String nextCursor) {}
