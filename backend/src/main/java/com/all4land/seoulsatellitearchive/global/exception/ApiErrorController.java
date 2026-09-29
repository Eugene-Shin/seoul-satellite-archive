package com.all4land.seoulsatellitearchive.global.exception;

import com.all4land.seoulsatellitearchive.global.exception.swagger.ErrorApi;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 스프링 부트 기본 /error 컨트롤러를 대체한다. 서블릿 필터처럼 @RestControllerAdvice가 닿지 않는 곳에서 난 예외도
// 기본 포맷({"timestamp", "status", ...}) 대신 ApiResponse로 응답하게 한다.
@Slf4j
@RestController
public class ApiErrorController implements ErrorController, ErrorApi {

    @Override
    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        CommonErrorCode errorCode = toErrorCode(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        log.warn(
                "[ErrorDispatch] status={}, path={}, exception={}",
                errorCode.getHttpStatus().value(),
                request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                exception == null ? "none" : exception.getClass().getName());
        return GlobalExceptionHandler.toResponse(errorCode, List.of());
    }

    // CommonErrorCode에 없는 상태(401, 403, 413, 503 등)는 500으로 바꿔 응답 상태와 본문 코드를 일치시킨다.
    // 원래 상태를 살려야 하는 경우가 생기면 CommonErrorCode에 해당 코드를 추가한다.
    private CommonErrorCode toErrorCode(Object statusCode) {
        return Arrays.stream(CommonErrorCode.values())
                .filter(code -> statusCode instanceof Integer status
                        && code.getHttpStatus().value() == status)
                .findFirst()
                .orElse(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
}
