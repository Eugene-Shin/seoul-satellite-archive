package com.all4land.seoulsatellitearchive.collection;

import com.all4land.seoulsatellitearchive.global.entity.BaseEntity;
import com.all4land.seoulsatellitearchive.provider.Provider;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageCollection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collection_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider")
    private Provider provider;

    private String code;

    private String title;

    private String constellation;

    private String productType;

    private String licenseName;

    private String licenseUrl;

    private String creditText;

    @ElementCollection
    @CollectionTable(name = "collection_band", joinColumns = @JoinColumn(name = "collection_id"))
    private List<CollectionBand> bands = new ArrayList<>();
}
