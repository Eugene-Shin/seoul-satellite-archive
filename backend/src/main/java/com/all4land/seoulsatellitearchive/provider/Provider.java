package com.all4land.seoulsatellitearchive.provider;

import com.all4land.seoulsatellitearchive.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    private MetadataSearchApi metadataSearchApi;

    private String metadataSearchUrl;

    @Enumerated(EnumType.STRING)
    private DownloadAuth downloadAuth;
}
