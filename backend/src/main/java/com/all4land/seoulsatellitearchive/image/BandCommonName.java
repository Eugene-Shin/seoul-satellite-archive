package com.all4land.seoulsatellitearchive.image;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 밴드 분류 사전(V32). image_file_band.common_name이 참조한다
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BandCommonName {

    @Id
    private String name;
}
