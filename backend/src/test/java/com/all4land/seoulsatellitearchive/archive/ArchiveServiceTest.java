package com.all4land.seoulsatellitearchive.archive;

import static org.assertj.core.api.Assertions.assertThat;

import com.all4land.seoulsatellitearchive.global.config.ArchiveProperties;
import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

// 출처 대신 JDK 내장 HTTP 서버로 받는다. 서버는 Range + If-Range(ETag가 같을 때만 206)를 따른다
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@Sql("/fixtures/search-images.sql")
class ArchiveServiceTest {

    private static final byte[] BODY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final String ETAG = "\"v1\"";

    @Autowired
    ArchiveService archiveService;

    @Autowired
    ArchiveProperties properties;

    @Autowired
    JdbcTemplate jdbc;

    // 테스트 트랜잭션 하나로 돌아서 JPA 변경을 SQL로 읽기 전에 내보낸다
    @Autowired
    EntityManager entityManager;

    private HttpServer server;
    private int status;
    private String contentType;
    private boolean truncate;
    private String receivedRange;
    private long fileId;

    @BeforeEach
    void setUp() throws IOException {
        status = 200;
        contentType = null;
        truncate = false;
        receivedRange = null;
        deleteRoot();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        fileId = jdbc.queryForObject(
                "SELECT file_id FROM image_file WHERE href = 'https://fx.test/s2/B04.tif'", Long.class);
        jdbc.update(
                "UPDATE image_file SET href = ? WHERE file_id = ?",
                "http://localhost:" + server.getAddress().getPort() + "/B04",
                fileId);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.stop(0);
        deleteRoot();
    }

    @Test
    void enqueueQueuesFilesWithPathsOnce() {
        long imageId =
                jdbc.queryForObject("SELECT image_id FROM satellite_image WHERE source_id = 'fx_s1'", Long.class);

        assertThat(archiveService.enqueue(imageId)).isEqualTo(1);
        assertThat(archiveService.enqueue(imageId)).isZero();
        assertThat(jdbc.queryForObject("""
                        SELECT a.archive_path FROM archive_file a JOIN image_file f ON f.file_id = a.file_id
                        WHERE f.image_id = ?
                        """, String.class, imageId))
                .isEqualTo("SENTINEL-1/SLC/ASC-127/20260111T093000_fx_s1/original.zip");
    }

    // 이미 정한 경로는 다시 계산하지 않으므로 새 파일이 그 이름을 피한다
    @Test
    void enqueueAvoidsNamesAlreadyTaken() {
        insertQueued("SENTINEL-2/L2A/_/20260108T020000_fx-s2-low/coastal_coastal.tif", null);
        long imageId =
                jdbc.queryForObject("SELECT image_id FROM satellite_image WHERE source_id = 'fx-s2-low'", Long.class);

        archiveService.enqueue(imageId);
        entityManager.flush();

        assertThat(jdbc.queryForObject("""
                        SELECT a.archive_path FROM archive_file a JOIN image_file f ON f.file_id = a.file_id
                        WHERE f.href = 'https://fx.test/s2/B01.tif'
                        """, String.class)).endsWith("/coastal_coastal_2.tif");
    }

    @Test
    void downloadsWholeFileAndRecordsEtag() throws IOException {
        insertQueued("t/a.tif", null);

        assertThat(archiveService.processNext()).isTrue();

        assertThat(row())
                .containsEntry("status", "ARCHIVED")
                .containsEntry("size_bytes", (long) BODY.length)
                .containsEntry("etag", ETAG)
                .containsEntry("extension", "tif");
        assertThat(Files.readAllBytes(properties.root().resolve("t/a.tif"))).isEqualTo(BODY);
        assertThat(archiveService.processNext()).isFalse();
    }

    @Test
    void resumesFromPartWithRange() throws IOException {
        insertQueued("t/a.tif", ETAG);
        writePart("t/a.tif", Arrays.copyOf(BODY, 4));

        archiveService.processNext();

        assertThat(receivedRange).isEqualTo("bytes=4-");
        assertThat(row()).containsEntry("status", "ARCHIVED");
        assertThat(Files.readAllBytes(properties.root().resolve("t/a.tif"))).isEqualTo(BODY);
    }

    // 끊긴 사이에 출처 파일이 바뀌었으면(ETag가 다르면) 처음부터 받는다
    @Test
    void restartsWhenSourceChanged() throws IOException {
        insertQueued("t/a.tif", "\"old\"");
        writePart("t/a.tif", "XXXX".getBytes(StandardCharsets.US_ASCII));

        archiveService.processNext();

        assertThat(row()).containsEntry("status", "ARCHIVED").containsEntry("etag", ETAG);
        assertThat(Files.readAllBytes(properties.root().resolve("t/a.tif"))).isEqualTo(BODY);
    }

    // 다 받지 못하면 실패로 두고 .part는 남겨서 재시도할 때 이어받는다
    @Test
    void failsOnTruncatedBodyAndKeepsPart() {
        truncate = true;
        insertQueued("t/a.tif", null);

        archiveService.processNext();

        assertThat(row()).containsEntry("status", "FAILED");
        assertThat(row().get("failure_reason")).isNotNull();
        assertThat(properties.root().resolve("t/a.tif.part")).exists();
    }

    @Test
    void failsOnErrorStatus() {
        status = 404;
        insertQueued("t/a.tif", null);

        archiveService.processNext();

        assertThat(row()).containsEntry("status", "FAILED");
        assertThat((String) row().get("failure_reason")).contains("404");
    }

    // 링크에 확장자가 없으면 응답 Content-Type으로 붙인다
    @Test
    void addsExtensionFromContentType() {
        contentType = "image/tiff";
        insertQueued("t/B04", null);

        archiveService.processNext();

        assertThat(row()).containsEntry("archive_path", "t/B04.tif").containsEntry("extension", "tif");
        assertThat(properties.root().resolve("t/B04.tif")).exists();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            receivedRange = exchange.getRequestHeaders().getFirst("Range");
            String ifRange = exchange.getRequestHeaders().getFirst("If-Range");
            if (status != 200) {
                exchange.sendResponseHeaders(status, -1);
                return;
            }
            exchange.getResponseHeaders().set("ETag", ETAG);
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }
            byte[] body = BODY;
            int code = 200;
            if (receivedRange != null && ETAG.equals(ifRange)) {
                int from = Integer.parseInt(receivedRange.substring("bytes=".length(), receivedRange.indexOf('-')));
                body = Arrays.copyOfRange(BODY, from, BODY.length);
                code = 206;
                exchange.getResponseHeaders()
                        .set("Content-Range", "bytes " + from + "-" + (BODY.length - 1) + "/" + BODY.length);
            }
            exchange.sendResponseHeaders(code, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(truncate ? Arrays.copyOf(body, body.length / 2) : body);
            } catch (IOException e) {
                // 덜 보낸 응답은 서버가 닫을 때 예외를 낸다(끊긴 다운로드 흉내)
            }
        }
    }

    private void insertQueued(String archivePath, String etag) {
        jdbc.update(
                "INSERT INTO archive_file (file_id, status, archive_path, etag) VALUES (?, 'QUEUED', ?, ?)",
                fileId,
                archivePath,
                etag);
    }

    private Map<String, Object> row() {
        entityManager.flush();
        return jdbc.queryForMap("SELECT * FROM archive_file WHERE file_id = ?", fileId);
    }

    private void writePart(String archivePath, byte[] bytes) throws IOException {
        Path part = properties.root().resolve(archivePath + ".part");
        Files.createDirectories(part.getParent());
        Files.write(part, bytes);
    }

    private void deleteRoot() throws IOException {
        if (!Files.exists(properties.root())) {
            return;
        }
        try (Stream<Path> paths = Files.walk(properties.root())) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> path.toFile().delete());
        }
    }
}
