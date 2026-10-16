package com.all4land.seoulsatellitearchive.image.swagger;

import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Image", description = "위성 영상 검색 API")
public interface ImageApi {

    @Operation(
            summary = "위성 영상 검색",
            description =
                    "모든 조건은 선택이며, 값이 없는 조건은 적용하지 않는다. 여러 값을 받는 조건은 하나라도 일치하면 포함한다. 첫 요청은 cursor 없이 보내고, 응답의 nextCursor를 다음 요청의 cursor로 넣어 이어 받는다. nextCursor가 null이면 끝이다. 커서는 같은 정렬(sorts)로만 쓸 수 있다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "검증 실패 (COMMON_400_002)")
    })
    ApiResponse<SliceResponse<ImageResponse>> search(ImageSearchRequest request);

    @Operation(summary = "검색 조건 선택지 조회", description = "드롭다운 선택지와 슬라이더 범위를 조회한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)")
    })
    ApiResponse<ImageFilterOptionsResponse> getFilterOptions();
}
