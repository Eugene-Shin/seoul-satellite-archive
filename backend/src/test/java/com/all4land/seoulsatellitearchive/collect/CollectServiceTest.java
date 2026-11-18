package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import com.all4land.seoulsatellitearchive.collect.StacClient.PageHandler;
import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

// 실행 기록과 영상을 각각 커밋하는 동작을 보려고 테스트 트랜잭션을 쓰지 않고 직접 지운다.
// 워커는 꺼 두고(application-test.yml) 대기열을 processNext로 직접 처리한다
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CollectServiceTest {

    private static final Duration LOOKBACK = Duration.ofDays(7);

    @Autowired
    CollectService collectService;

    @Autowired
    CollectRunRepository collectRunRepository;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    StacClient stacClient;

    private final List<Long> createdTargetIds = new ArrayList<>();

    private long targetId;

    // 컬렉션마다 대상이 하나라서 c1은 seed 서울 대상을 쓴다
    @BeforeEach
    void setUp() {
        targetId = jdbc.queryForObject("""
                SELECT t.target_id FROM collect_target t
                JOIN image_collection c ON c.collection_id = t.collection_id
                WHERE c.code = 'sentinel-2-c1-l2a'
                """, Long.class);
    }

    // seed 대상은 남기고 그 실행 이력·수집 영상과, 이 테스트가 만든 대상만 지운다
    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM satellite_image WHERE collect_target_id = ?", targetId);
        jdbc.update("DELETE FROM collect_run WHERE target_id = ?", targetId);
        for (Long created : createdTargetIds) {
            jdbc.update("DELETE FROM satellite_image WHERE collect_target_id = ?", created);
            jdbc.update("DELETE FROM collect_target WHERE target_id = ?", created);
        }
        createdTargetIds.clear();
    }

    @Test
    void insertsNewImagesWithFiles() throws IOException {
        stubSearch(items());

        CollectRun run = runOneOff();

        assertThat(run.getStatus()).isEqualTo(CollectRunStatus.SUCCEEDED);
        assertThat(run.getFound()).isEqualTo(2);
        assertThat(run.getInserted()).isEqualTo(2);
        assertThat(run.getWindowFrom()).isEqualTo(run.getWindowTo().minus(LOOKBACK));
        assertThat(run.getStartedAt()).isNotNull();
        assertThat(run.getHeartbeatAt()).isNotNull();
        assertThat(count("SELECT count(*) FROM satellite_image WHERE collect_target_id = ?", targetId))
                .isEqualTo(2);
        assertThat(count("""
                        SELECT count(*) FROM image_file f JOIN satellite_image i ON i.image_id = f.image_id
                        WHERE i.source_id = 'S2B_T52SCG_20261028T022157_L2A'
                        """)).isEqualTo(23);
    }

    // 단발 실행의 성공은 다음 조회 시작점이 되지 않는다
    @Test
    void defaultWindowContinuesFromLastScheduledSuccessOnly() throws IOException {
        Instant scheduledEnd = Instant.parse("2026-11-10T00:00:00Z");
        insertSucceededRun("SCHEDULED", scheduledEnd);
        insertSucceededRun("ONE_OFF", Instant.parse("2026-11-15T00:00:00Z"));
        stubSearch(items());

        CollectRun run = runOneOff();

        assertThat(run.getWindowFrom()).isEqualTo(scheduledEnd.minus(LOOKBACK));
    }

    @Test
    void oneOffKeepsGivenWindow() throws IOException {
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-02-01T00:00:00Z");
        stubSearch(items());

        Long runId = collectService.enqueue(targetId, CollectRunType.ONE_OFF, from, to);
        collectService.processNext();

        CollectRun run = findRun(runId);
        assertThat(run.getWindowFrom()).isEqualTo(from);
        assertThat(run.getWindowTo()).isEqualTo(to);
    }

    @Test
    void rerunSkipsUnchangedAndUpdatesChanged() throws IOException {
        stubSearch(items());
        runOneOff();
        assertThat(runOneOff().getUpdated()).isZero();

        List<JsonNode> changed = items();
        ((ObjectNode) changed.get(0).path("properties")).put("updated", "2026-11-01T00:00:00Z");
        stubSearch(changed);

        CollectRun run = runOneOff();

        assertThat(run.getInserted()).isZero();
        assertThat(run.getUpdated()).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM satellite_image WHERE source_updated_at = '2026-11-01T00:00:00Z'"))
                .isEqualTo(1);
    }

    @Test
    void skipsBrokenItemAndSavesOthers() throws IOException {
        List<JsonNode> items = items();
        ((ObjectNode) items.get(0)).remove("geometry");
        stubSearch(items);

        CollectRun run = runOneOff();

        assertThat(run.getStatus()).isEqualTo(CollectRunStatus.SUCCEEDED);
        assertThat(run.getInserted()).isEqualTo(1);
        assertThat(run.getSkipped()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT source_id FROM collect_run_skip WHERE run_id = ?", String.class, run.getId()))
                .isEqualTo("S2B_T52SCG_20261028T022157_L2A");
    }

    @Test
    void failedSearchIsRecorded() {
        doThrow(new ResourceAccessException("connection refused"))
                .when(stacClient)
                .search(any(), any(), any(), any(), any(), any());

        CollectRun failed = runOneOff();

        assertThat(failed.getStatus()).isEqualTo(CollectRunStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("connection refused");
        assertThat(failed.getFinishedAt()).isNotNull();
        assertThat(count("SELECT count(*) FROM satellite_image WHERE collect_target_id = ?", targetId))
                .isZero();
    }

    @Test
    void rejectsCollectionWithoutMapper() {
        long legacyTarget = insertTarget("sentinel-2-l2a");

        assertThatThrownBy(() -> collectService.enqueue(legacyTarget, CollectRunType.ONE_OFF, null, null))
                .isInstanceOf(BusinessException.class);
        assertThat(count("SELECT count(*) FROM collect_run WHERE target_id = ?", legacyTarget))
                .isZero();
    }

    @Test
    void rejectsSecondActiveRunOfSameTarget() {
        collectService.enqueue(targetId, CollectRunType.ONE_OFF, null, null);

        assertThatThrownBy(() -> collectService.enqueue(targetId, CollectRunType.SCHEDULED, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 대기 중이거나 실행 중");
    }

    @Test
    void cancelingQueuedRunCancelsImmediately() {
        Long runId = collectService.enqueue(targetId, CollectRunType.ONE_OFF, null, null);

        collectService.requestCancel(runId);

        assertThat(findRun(runId).getStatus()).isEqualTo(CollectRunStatus.CANCELED);
        assertThat(collectService.processNext()).isFalse();
    }

    // 실행 중 취소는 다음 확인 지점(영상마다)에서 멈추고, 그때까지 저장한 영상은 남는다
    @Test
    void cancelingRunningRunStopsAtNextCheck() throws IOException {
        List<JsonNode> items = items();
        Long runId = collectService.enqueue(targetId, CollectRunType.ONE_OFF, null, null);
        answerSearch(pageHandler -> {
            pageHandler.handle(List.of(items.get(0)));
            collectService.requestCancel(runId);
            pageHandler.handle(List.of(items.get(1)));
        });

        collectService.processNext();

        CollectRun run = findRun(runId);
        assertThat(run.getStatus()).isEqualTo(CollectRunStatus.CANCELED);
        assertThat(run.getInserted()).isEqualTo(1);
        assertThat(run.getCancelRequestedAt()).isNotNull();
        assertThat(count("SELECT count(*) FROM satellite_image WHERE collect_target_id = ?", targetId))
                .isEqualTo(1);
    }

    // 확인하기 전에 끝까지 처리했으면 취소 요청이 있어도 성공이다
    @Test
    void cancelRequestedAfterLastItemEndsAsSucceeded() throws IOException {
        List<JsonNode> items = items();
        Long runId = collectService.enqueue(targetId, CollectRunType.ONE_OFF, null, null);
        answerSearch(pageHandler -> {
            pageHandler.handle(items);
            collectService.requestCancel(runId);
        });

        collectService.processNext();

        CollectRun run = findRun(runId);
        assertThat(run.getStatus()).isEqualTo(CollectRunStatus.SUCCEEDED);
        assertThat(run.getInserted()).isEqualTo(2);
    }

    @Test
    void cancelingFinishedRunConflicts() throws IOException {
        stubSearch(items());
        CollectRun run = runOneOff();

        assertThatThrownBy(() -> collectService.requestCancel(run.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 끝난 실행");
    }

    private CollectRun runOneOff() {
        Long runId = collectService.enqueue(targetId, CollectRunType.ONE_OFF, null, null);
        collectService.processNext();
        return findRun(runId);
    }

    private CollectRun findRun(Long runId) {
        return collectRunRepository.findById(runId).orElseThrow();
    }

    // 정기 실행은 스케줄러(C-6)가 생기기 전까지 SQL로 넣는다
    private void insertSucceededRun(String runType, Instant windowTo) {
        jdbc.update("""
                INSERT INTO collect_run (target_id, run_type, status, window_from, window_to, started_at, finished_at)
                VALUES (?, ?, 'SUCCEEDED', ?::timestamptz - INTERVAL '7 days', ?::timestamptz, now(), now())
                """, targetId, runType, windowTo.toString(), windowTo.toString());
    }

    private long insertTarget(String collectionCode) {
        Long created = jdbc.queryForObject("""
                INSERT INTO collect_target (collection_id, aoi, schedule_cron)
                SELECT collection_id,
                       ST_GeomFromText('MULTIPOLYGON(((126.9 37.5, 127.1 37.5, 127.1 37.6, 126.9 37.5)))', 4326),
                       '0 0 3 * * *'
                FROM image_collection
                WHERE provider = 'earth-search' AND code = ?
                RETURNING target_id
                """, Long.class, collectionCode);
        createdTargetIds.add(created);
        return created;
    }

    // 실제 클라이언트처럼 페이지 하나로 넘기고 끝까지 받았다고 돌려준다
    private void stubSearch(List<JsonNode> items) {
        answerSearch(pageHandler -> pageHandler.handle(items));
    }

    // when(mock.search(...))는 스텁을 걸 때 이전 스텁을 실행하므로 doAnswer로 건다
    private void answerSearch(Consumer<PageHandler> pages) {
        doAnswer(invocation -> {
                    pages.accept(invocation.getArgument(5));
                    return true;
                })
                .when(stacClient)
                .search(any(), any(), any(), any(), any(), any());
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private static List<JsonNode> items() throws IOException {
        try (InputStream in = CollectServiceTest.class.getResourceAsStream("/collect/sentinel-2-c1-l2a-search.json")) {
            List<JsonNode> items = new ArrayList<>();
            JsonMapper.shared().readTree(in).path("features").forEach(items::add);
            return items;
        }
    }
}
