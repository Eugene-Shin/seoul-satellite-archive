package com.all4land.seoulsatellitearchive.image.annotation;

import com.all4land.seoulsatellitearchive.image.ImageSort;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;

public class UniqueSortPropertiesValidator implements ConstraintValidator<UniqueSortProperties, List<ImageSort>> {

    @Override
    public boolean isValid(List<ImageSort> sorts, ConstraintValidatorContext context) {
        if (sorts == null) {
            return true;
        }
        List<String> properties = sorts.stream()
                .filter(Objects::nonNull)
                .map(ImageSort::getProperty)
                .toList();
        return properties.stream().distinct().count() == properties.size();
    }
}
