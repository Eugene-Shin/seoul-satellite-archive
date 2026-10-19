package com.all4land.seoulsatellitearchive.image.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidBboxValidator.class)
@Size(min = 4, max = 4, message = "minLon,minLat,maxLon,maxLat 4개 값이어야 합니다.") @Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBbox {
    String message() default "경도는 -180~180, 위도는 -90~90, 최소값은 최대값 이하여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
