-- 실행은 대기열(QUEUED)에 넣고 워커가 가져가 실행한다. 다음 조회 시작점은 성공한 정기 실행에서만 찾는다
ALTER TABLE collect_run
    ADD COLUMN run_type            VARCHAR(20) NOT NULL CHECK (run_type IN ('SCHEDULED', 'ONE_OFF')),
    ADD COLUMN created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),  -- 대기열에 들어간 시각
    ADD COLUMN cancel_requested_at TIMESTAMPTZ,  -- 실행 중 취소 요청. 상태는 워커가 확인하고 바꾼다
    ALTER COLUMN window_from DROP NOT NULL,
    ALTER COLUMN window_to DROP NOT NULL,
    ALTER COLUMN started_at DROP NOT NULL,
    ALTER COLUMN started_at DROP DEFAULT,
    DROP CONSTRAINT collect_run_status_check,
    ADD CONSTRAINT collect_run_status_check
        CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED')),
    -- 조회 구간은 워커가 시작할 때 정한다(단발 실행은 등록할 때 줄 수 있다). 시작 전에 취소되면 비어 있다
    ADD CONSTRAINT ck_collect_run_window_set
        CHECK ((window_from IS NULL) = (window_to IS NULL)
               AND (status IN ('QUEUED', 'CANCELED') OR window_from IS NOT NULL)),
    ADD CONSTRAINT ck_collect_run_started
        CHECK (status IN ('QUEUED', 'CANCELED') OR started_at IS NOT NULL);

-- 같은 대상을 동시에 두 번 수집하지 않는다
DROP INDEX uq_collect_run_running;
CREATE UNIQUE INDEX uq_collect_run_active ON collect_run (target_id) WHERE status IN ('QUEUED', 'RUNNING');

-- 시작하지 않은 실행은 started_at이 없어서 이력은 등록 순서로 본다
DROP INDEX idx_collect_run_target_started;
CREATE INDEX idx_collect_run_target_created ON collect_run (target_id, created_at DESC);

-- 워커가 폴링할 때 대기 중인 행만 본다
CREATE INDEX idx_collect_run_queued ON collect_run (created_at) WHERE status = 'QUEUED';
