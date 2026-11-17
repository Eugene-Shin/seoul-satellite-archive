package com.all4land.seoulsatellitearchive.collect.swagger;

import com.all4land.seoulsatellitearchive.collect.dto.CollectRunRequest;
import com.all4land.seoulsatellitearchive.collect.dto.CollectRunResponse;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Collect", description = "메타데이터 수집 실행 API")
public interface CollectRunApi {

    @Operation(
            summary = "단발 수집 실행 등록",
            description = "대기열에 넣고 바로 돌려준다. 워커가 몇 초 안에 가져가 실행하며, 진행은 실행 상태 조회로 확인한다. 단발 실행은 다음 정기 수집의 시작점에 영향을 주지 않는다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "접수 (COMMON_202_001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "수집할 수 없는 컬렉션, 잘못된 조회 구간"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 수집 대상"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "같은 대상의 실행이 이미 대기 중이거나 실행 중")
    })
    ApiResponse<CollectRunResponse> enqueue(Long targetId, CollectRunRequest request);

    @Operation(summary = "수집 실행 상태 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 실행")
    })
    ApiResponse<CollectRunResponse> getRun(Long runId);

    @Operation(
            summary = "수집 실행 취소 요청",
            description = "대기 중이면 바로 CANCELED가 된다. 실행 중이면 cancelRequestedAt만 기록하고, 워커가 다음 확인 지점(영상마다, 다음 페이지 전)에서 "
                    + "멈추고 CANCELED로 바꾼다. 확인하기 전에 끝까지 처리했으면 SUCCEEDED로 끝난다. 같은 요청을 다시 보내도 결과는 같다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 실행"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 끝난 실행")
    })
    ApiResponse<CollectRunResponse> cancel(Long runId);
}
