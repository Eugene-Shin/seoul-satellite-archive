package com.all4land.seoulsatellitearchive.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<String> errorDetail;

    @JsonIgnore
    private final HttpStatus httpStatus;

    public static <T> ApiResponse<T> success(BaseSuccessCode sc, T data) {
        return new ApiResponse<>(true, sc.getCode(), sc.getMessage(), data, List.of(), sc.getHttpStatus());
    }

    public static ApiResponse<Void> success(BaseSuccessCode sc) {
        return success(sc, null);
    }

    public static ApiResponse<Void> fail(BaseErrorCode ec) {
        return fail(ec, List.of());
    }

    public static ApiResponse<Void> fail(BaseErrorCode ec, List<String> errorDetail) {
        return new ApiResponse<>(
                false, ec.getCode(), ec.getMessage(), null, List.copyOf(errorDetail), ec.getHttpStatus());
    }
}
