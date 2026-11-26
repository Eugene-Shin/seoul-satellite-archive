package com.all4land.seoulsatellitearchive.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    JdbcTemplate jdbc;

    private long fileId;

    @BeforeEach
    void setUp() {
        fileId = jdbc.queryForObject(
                "SELECT file_id FROM image_file WHERE href = 'https://fx.test/s2/B04.tif'", Long.class);
    }

    @Test
    void archivedNeedsPathSizeAndTime() {
        assertThatThrownBy(() -> insert("ARCHIVED")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void failedNeedsReason() {
        assertThatThrownBy(() -> insert("FAILED")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oneArchiveFilePerImageFile() {
        insert("QUEUED");

        assertThatThrownBy(() -> insert("QUEUED")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingImageDeletesArchiveFile() {
        insert("QUEUED");

        jdbc.update(
                "DELETE FROM satellite_image WHERE image_id = (SELECT image_id FROM image_file WHERE file_id = ?)",
                fileId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM archive_file", Long.class))
                .isZero();
    }

    private void insert(String status) {
        jdbc.update("INSERT INTO archive_file (file_id, status) VALUES (?, ?)", fileId, status);
    }
}
