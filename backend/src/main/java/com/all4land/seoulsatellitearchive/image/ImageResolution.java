package com.all4land.seoulsatellitearchive.image;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

// image_resolution 뷰. 영상당 한 행이고, 해상도 값이 없는 영상은 행이 없다
@Getter
@Entity
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageResolution {

    @Id
    private Long imageId;

    private BigDecimal minMeters;

    private BigDecimal maxMeters;
}
