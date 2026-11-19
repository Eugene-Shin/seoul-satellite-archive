package com.all4land.seoulsatellitearchive.collect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.locationtech.jts.geom.MultiPolygon;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AoiPreset {

    public static final String SEOUL = "seoul";

    @Id
    private String code;

    private String name;

    private MultiPolygon geom;

    @Formula("ST_AsGeoJSON(geom)")
    private String geomGeoJson;

    private String source;
}
