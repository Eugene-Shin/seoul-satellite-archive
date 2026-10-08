package com.all4land.seoulsatellitearchive.provider;

import com.all4land.seoulsatellitearchive.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Provider extends BaseEntity {

    @Id
    private String code;

    private String name;

    private boolean isPaid;
}
