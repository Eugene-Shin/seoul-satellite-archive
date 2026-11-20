package com.all4land.seoulsatellitearchive.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@Sql("/fixtures/search-images.sql")
class ArchiveFileTableTest {

    private static final String HREF = "https://fx.test/s2/B04.tif";

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void archivedNeedsPathSizeAndTime() {
        assertThatThrownBy(() -> insert("ARCHIVED", HREF)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void failedNeedsReason() {
        assertThatThrownBy(() -> insert("FAILED", HREF)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameHrefIsOneFile() {
        insert("QUEUED", HREF);

        assertThatThrownBy(() -> insert("QUEUED", HREF)).isInstanceOf(DataIntegrityViolationException.class);
    }

    // 아카이브 행을 지우면 영상 파일은 남고 연결만 끊긴다
    @Test
    void deletingArchiveFileUnlinksImageFile() {
        long archiveFileId = insert("QUEUED", HREF);
        jdbc.update("UPDATE image_file SET archive_file_id = ? WHERE href = ?", archiveFileId, HREF);

        jdbc.update("DELETE FROM archive_file WHERE archive_file_id = ?", archiveFileId);

        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM image_file WHERE href = ? AND archive_file_id IS NULL", Long.class, HREF))
                .isOne();
    }

    private long insert(String status, String href) {
        return jdbc.queryForObject(
                "INSERT INTO archive_file (href, status) VALUES (?, ?) RETURNING archive_file_id",
                Long.class,
                href,
                status);
    }
}
