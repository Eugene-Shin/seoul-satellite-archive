-- 수집기가 주기적으로 조회할 컬렉션과 영역. 다음 조회 구간은 마지막 성공 실행의 window_to에서 이어진다
CREATE TABLE collect_target (
    target_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collection_id BIGINT                       NOT NULL REFERENCES image_collection (collection_id),
    aoi           geometry(MultiPolygon, 4326) NOT NULL,
    schedule_cron VARCHAR(100)                 NOT NULL,  -- Spring CronExpression 형식(초 포함 6자리)
    lookback      INTERVAL                     NOT NULL DEFAULT '7 days',  -- 늦게 공개·갱신되는 영상을 다시 조회할 범위
    enabled       BOOLEAN                      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ                  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ                  NOT NULL DEFAULT now()
);

CREATE INDEX idx_collect_target_aoi ON collect_target USING GIST (aoi);

CREATE TABLE collect_run (
    run_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    target_id      BIGINT      NOT NULL REFERENCES collect_target (target_id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    window_from    TIMESTAMPTZ NOT NULL,
    window_to      TIMESTAMPTZ NOT NULL,
    found          INTEGER     NOT NULL DEFAULT 0,
    inserted       INTEGER     NOT NULL DEFAULT 0,
    updated        INTEGER     NOT NULL DEFAULT 0,
    skipped        INTEGER     NOT NULL DEFAULT 0,
    failure_reason TEXT,
    started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at    TIMESTAMPTZ,

    CONSTRAINT ck_collect_run_window CHECK (window_from < window_to)
);

CREATE INDEX idx_collect_run_target_started ON collect_run (target_id, started_at DESC);
-- 같은 대상을 동시에 두 번 수집하지 않는다
CREATE UNIQUE INDEX uq_collect_run_running ON collect_run (target_id) WHERE status = 'RUNNING';

-- 변환에 실패해 건너뛴 영상. 이력이라 고치지 않고, 이후 실행에서 성공하면 그 실행에 센다
CREATE TABLE collect_run_skip (
    run_id    BIGINT       NOT NULL REFERENCES collect_run (run_id) ON DELETE CASCADE,
    source_id VARCHAR(200) NOT NULL,
    reason    TEXT         NOT NULL,
    PRIMARY KEY (run_id, source_id)
);
