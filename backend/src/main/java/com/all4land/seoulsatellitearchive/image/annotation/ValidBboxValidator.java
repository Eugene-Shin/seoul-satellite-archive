package com.all4land.seoulsatellitearchive.image.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class ValidBboxValidator implements ConstraintValidator<ValidBbox, List<Double>> {

    private static final double MAX_LON = 180;
    private static final double MAX_LAT = 90;
    private static final String EMPTY_VALUE_MESSAGE = "빈 값이 있으면 안 됩니다.";

    @Override
    public boolean isValid(List<Double> bbox, ConstraintValidatorContext context) {
        if (bbox == null || bbox.size() != 4) {
            return true;
        }
        if (bbox.contains(null)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(EMPTY_VALUE_MESSAGE).addConstraintViolation();
            return false;
        }
        double minLon = bbox.get(0);
        double minLat = bbox.get(1);
        double maxLon = bbox.get(2);
        double maxLat = bbox.get(3);
        return isLon(minLon) && isLon(maxLon) && isLat(minLat) && isLat(maxLat) && minLon <= maxLon && minLat <= maxLat;
    }

    private static boolean isLon(double value) {
        return Math.abs(value) <= MAX_LON;
    }

    private static boolean isLat(double value) {
        return Math.abs(value) <= MAX_LAT;
    }
}
