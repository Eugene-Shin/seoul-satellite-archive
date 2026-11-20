package com.all4land.seoulsatellitearchive.archive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "archive_file_id")
    private Long id;

    private String href;

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
}
