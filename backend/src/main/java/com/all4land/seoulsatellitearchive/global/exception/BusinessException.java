package com.all4land.seoulsatellitearchive.global.exception;

import com.all4land.seoulsatellitearchive.global.response.BaseErrorCode;
import java.util.Objects;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public BusinessException(BaseErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessException(BaseErrorCode errorCode, String detailMessage) {
        super(resolveMessage(errorCode, detailMessage));
        this.errorCode = errorCode;
    }

    private static String resolveMessage(BaseErrorCode errorCode, String detailMessage) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return detailMessage == null || detailMessage.isBlank() ? errorCode.getMessage() : detailMessage;
    }
}
