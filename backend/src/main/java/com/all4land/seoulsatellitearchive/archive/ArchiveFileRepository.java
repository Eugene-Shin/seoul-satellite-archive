package com.all4land.seoulsatellitearchive.archive;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArchiveFileRepository extends JpaRepository<ArchiveFile, Long> {

    @Query(value = """
            SELECT * FROM archive_file
            WHERE status = 'QUEUED'
            ORDER BY created_at, file_id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<ArchiveFile> lockNextQueued();

    @Query("select a from ArchiveFile a where a.file.image.id = :imageId")
    List<ArchiveFile> findByImage(Long imageId);
}
