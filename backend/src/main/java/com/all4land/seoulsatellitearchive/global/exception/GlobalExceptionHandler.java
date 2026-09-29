package com.all4land.seoulsatellitearchive.global.exception;

import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.global.response.BaseErrorCode;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("[BusinessException] code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return toResponse(ex.getErrorCode(), List.of(ex.getMessage()));
    }

    // @Valid 검증 실패. MethodArgumentNotValidException도 BindException의 하위 타입이라 여기서 함께 처리된다.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBind(BindException ex) {
        List<String> detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return toResponse(CommonErrorCode.VALIDATION_ERROR, detail);
    }

    // 컨트롤러 메서드 파라미터(@RequestParam, @PathVariable 등)에 붙은 제약 조건 위반
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> detail = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        return toResponse(CommonErrorCode.VALIDATION_ERROR, detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return toResponse(CommonErrorCode.VALIDATION_ERROR, detail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return toResponse(CommonErrorCode.VALIDATION_ERROR, List.of(ex.getName() + ": 타입이 올바르지 않습니다."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return toResponse(CommonErrorCode.VALIDATION_ERROR, List.of(ex.getParameterName() + ": 필수 파라미터가 누락되었습니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("[HttpMessageNotReadable]", ex);
        return toResponse(CommonErrorCode.BAD_REQUEST, List.of("요청 본문(JSON)을 올바르게 작성해 주세요."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return toResponse(CommonErrorCode.METHOD_NOT_ALLOWED, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return toResponse(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return toResponse(CommonErrorCode.NOT_ACCEPTABLE, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return toResponse(CommonErrorCode.NOT_FOUND, List.of());
    }

    // 라이브러리가 던진 예외일 수 있어 메시지에 내부 정보가 섞일 수 있다. 상세 내용은 로그로만 남긴다.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("[IllegalStateException]", ex);
        return toResponse(CommonErrorCode.CONFLICT, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return toResponse(CommonErrorCode.INTERNAL_SERVER_ERROR, List.of());
    }

    // Content-Type을 JSON으로 고정한다. 요청의 Accept가 JSON이 아니면(예: application/xml) 에러 응답을 쓰는
    // 도중에 406이 다시 터져 원래 상태 코드가 사라지기 때문이다.
    static ResponseEntity<ApiResponse<Void>> toResponse(BaseErrorCode errorCode, List<String> errorDetail) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(errorCode, errorDetail));
    }
}
