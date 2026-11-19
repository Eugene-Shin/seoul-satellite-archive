-- 실행 중인 워커가 살아 있다는 표시. 오래 갱신되지 않은 RUNNING은 서버가 죽은 것으로 보고 스케줄러가 FAILED로 정리한다
ALTER TABLE collect_run ADD COLUMN heartbeat_at TIMESTAMPTZ;
