package com.all4land.seoulsatellitearchive.global.exception.swagger;

import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

// 화면에서 호출하는 API가 아니라 서블릿 에러 디스패치의 종착지라 Swagger 목록에서는 숨긴다.
@Hidden
public interface ErrorApi {

    @Operation(summary = "에러 디스패치 응답", description = "예외 처리기가 닿지 않는 곳(서블릿 필터 등)에서 난 에러를 ApiResponse 형식으로 응답한다.")
    ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request);
}
