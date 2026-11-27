package com.all4land.seoulsatellitearchive.archive;

import com.all4land.seoulsatellitearchive.image.ImageFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchiveFile {

    @Id
    private Long fileId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private ImageFile file;

    @Enumerated(EnumType.STRING)
    private ArchiveFileStatus status;

    private String archivePath;

    private Long sizeBytes;

    private String etag;

    private String extension;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<ArchiveEntry> entries;

    private String failureReason;

    private Instant archivedAt;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    // 경로는 요청할 때 영상 파일 전체로 정한다(겹친 이름 번호)
    public ArchiveFile(ImageFile file, String archivePath) {
        this.file = file;
        this.archivePath = archivePath;
        this.status = ArchiveFileStatus.QUEUED;
    }

    public void start() {
        this.status = ArchiveFileStatus.ARCHIVING;
    }

    // 다음 이어받기의 If-Range
    public void recordEtag(String etag) {
        this.etag = etag;
    }

    // 확장자가 없으면(빈 문자열) 다른 칸처럼 NULL로 둔다
    public void archive(String archivePath, String extension, long sizeBytes, Instant archivedAt) {
        this.status = ArchiveFileStatus.ARCHIVED;
        this.archivePath = archivePath;
        this.extension = extension.isEmpty() ? null : extension;
        this.sizeBytes = sizeBytes;
        this.archivedAt = archivedAt;
        this.failureReason = null;
    }

    public void fail(String reason) {
        this.status = ArchiveFileStatus.FAILED;
        this.failureReason = reason;
    }
}
