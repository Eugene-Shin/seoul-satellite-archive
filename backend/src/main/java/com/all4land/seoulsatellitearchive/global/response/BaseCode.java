package com.all4land.seoulsatellitearchive.global.response;

import org.springframework.http.HttpStatus;

public interface BaseCode {

    String getCode();

    String getMessage();

    HttpStatus getHttpStatus();
}
