package com.all4land.seoulsatellitearchive.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonSuccessCode implements BaseSuccessCode {
    OK(HttpStatus.OK, "COMMON_200_001", "성공적으로 요청을 처리했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
