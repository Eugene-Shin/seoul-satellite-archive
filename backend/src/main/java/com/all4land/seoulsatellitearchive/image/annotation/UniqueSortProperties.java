package com.all4land.seoulsatellitearchive.image.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = UniqueSortPropertiesValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueSortProperties {
    String message() default "같은 정렬 기준은 한 번만 지정할 수 있습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
