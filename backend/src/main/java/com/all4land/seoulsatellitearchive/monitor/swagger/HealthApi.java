package com.all4land.seoulsatellitearchive.monitor.swagger;

import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@Tag(name = "Health", description = "서버 상태 확인 API")
public interface HealthApi {

    @Operation(summary = "헬스 체크", description = "서버가 요청을 처리할 수 있는 상태인지 확인한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상 (COMMON_200_001)")
    })
    ApiResponse<Map<String, String>> health();
}
