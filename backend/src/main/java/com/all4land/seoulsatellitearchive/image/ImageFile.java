package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.archive.ArchiveFile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_file_id")
    private ArchiveFile archiveFile;

    @ElementCollection
    @CollectionTable(name = "image_file_band", joinColumns = @JoinColumn(name = "file_id"))
    @OrderBy("bandIndex")
    private List<ImageFileBand> bands = new ArrayList<>();

    // 링크만으로는 형식·픽셀 간격·밴드를 알 수 없어서 비워 둔다
    public ImageFile(SatelliteImage image, String href) {
        String fileName = href.substring(href.lastIndexOf('/') + 1);
        this.image = image;
        this.assetKey =
                fileName.length() > ASSET_KEY_MAX_LENGTH ? fileName.substring(0, ASSET_KEY_MAX_LENGTH) : fileName;
        this.role = ImageFileRole.fromHref(href);
        this.href = href;
    }

    public ImageFile(SatelliteImage image, String href, ImageFileDetails details) {
        this.image = image;
        this.href = href;
        replaceDetails(details);
    }

    // 같은 링크의 행은 남겨 두고(아카이빙 정보 보존) 출처가 준 값만 바꾼다
    public void replaceDetails(ImageFileDetails details) {
        this.assetKey = details.assetKey();
        this.role = details.role();
        this.mediaType = details.mediaType();
        this.pixelSpacingMeters = details.pixelSpacingMeters();
        this.bands.clear();
        this.bands.addAll(details.bands());
    }
}
