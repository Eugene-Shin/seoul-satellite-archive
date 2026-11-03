package com.all4land.seoulsatellitearchive.collection.swagger;

import com.all4land.seoulsatellitearchive.collection.dto.CollectionResponse;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Collection", description = "영상 컬렉션 API")
public interface CollectionApi {

    @Operation(summary = "컬렉션 목록 조회", description = "등록·수정 폼의 컬렉션 선택지. 제공 기관, 제목 순으로 정렬한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)")
    })
    ApiResponse<List<CollectionResponse>> findAll();
}
