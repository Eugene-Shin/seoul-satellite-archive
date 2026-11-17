package com.all4land.seoulsatellitearchive.collect.swagger;

import com.all4land.seoulsatellitearchive.collect.dto.AoiPresetResponse;
import com.all4land.seoulsatellitearchive.collect.dto.CollectTargetRequest;
import com.all4land.seoulsatellitearchive.collect.dto.CollectTargetResponse;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Collect Target", description = "메타데이터 수집 대상 관리 API")
public interface CollectTargetApi {

    @Operation(summary = "수집 대상 목록")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)")
    })
    ApiResponse<List<CollectTargetResponse>> findAll();

    @Operation(summary = "새 수집 대상의 영역 기본값", description = "서울특별시 경계. 화면에 출처 문구(source)를 함께 표시한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)")
    })
    ApiResponse<AoiPresetResponse> getDefaultAoi();

    @Operation(summary = "수집 대상 등록")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 (COMMON_201_001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "없거나 수집할 수 없는 컬렉션, 잘못된 영역·cron·lookback")
    })
    ApiResponse<CollectTargetResponse> create(CollectTargetRequest request);

    @Operation(summary = "수집 대상 전체 수정", description = "컬렉션은 바꿀 수 없다. 영역을 넓히면 새로 들어온 지역은 lookback 기간만큼만 받는다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "컬렉션 변경, 잘못된 영역·cron·lookback"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 수집 대상")
    })
    ApiResponse<CollectTargetResponse> update(Long targetId, CollectTargetRequest request);

    @Operation(summary = "수집 대상 삭제", description = "실행 이력은 함께 지워지고, 수집한 영상은 남아서 수정할 수 있는 일반 영상이 된다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 수집 대상"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "대기 중이거나 실행 중인 수집이 있음")
    })
    ApiResponse<Void> delete(Long targetId);
}
