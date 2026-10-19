package com.all4land.seoulsatellitearchive.image.swagger;

import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageDetailResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Image", description = "위성 영상 검색 및 메타데이터 관리 API")
public interface ImageApi {

    @Operation(
            summary = "위성 영상 검색",
            description =
                    "모든 조건은 선택이며, 값이 없는 조건은 적용하지 않는다. 여러 값을 받는 조건은 하나라도 일치하면 포함한다. 첫 요청은 cursor 없이 보내고, 응답의 nextCursor를 다음 요청의 cursor로 넣어 이어 받는다. nextCursor가 null이면 끝이다. 커서는 같은 정렬(sorts)로만 쓸 수 있다. bbox는 WGS84 경위도 minLon,minLat,maxLon,maxLat 4개 값이며, footprint가 이 사각형과 겹치는 영상을 찾는다.")
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

    @Operation(summary = "메타데이터 등록")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "제공 기관 내 sourceId 중복")
    })
    ApiResponse<Void> create(ImageWriteRequest request);

    @Operation(summary = "수정 폼용 메타데이터 상세 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모든 입력값과 ID·시각 반환"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 영상")
    })
    ApiResponse<ImageDetailResponse> detail(Long imageId);

    @Operation(summary = "메타데이터 전체 수정", description = "선택 항목을 생략하거나 null로 보내면 비운다. rawMetadata는 {}로 저장한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 영상"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "제공 기관 내 sourceId 중복")
    })
    ApiResponse<Void> update(Long imageId, ImageWriteRequest request);

    @Operation(summary = "영상 DB 행 삭제", description = "실제 파일은 삭제하지 않는다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 영상")
    })
    ApiResponse<Void> delete(Long imageId);
}
