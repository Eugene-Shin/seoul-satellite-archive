package com.all4land.seoulsatellitearchive.image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageFile {

    private static final int ASSET_KEY_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private SatelliteImage image;

    private String assetKey;

    @Enumerated(EnumType.STRING)
    private ImageFileRole role;

    private String href;

    private String mediaType;

    private BigDecimal pixelSpacingMeters;

    // 링크만으로는 형식·픽셀 간격·밴드를 알 수 없어서 비워 둔다
    public ImageFile(SatelliteImage image, String href) {
        String fileName = href.substring(href.lastIndexOf('/') + 1);
        this.image = image;
        this.assetKey =
                fileName.length() > ASSET_KEY_MAX_LENGTH ? fileName.substring(0, ASSET_KEY_MAX_LENGTH) : fileName;
        this.role = ImageFileRole.fromHref(href);
        this.href = href;
    }
}
