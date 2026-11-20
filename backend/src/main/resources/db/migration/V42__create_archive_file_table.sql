-- 아카이빙한 실제 파일. 링크가 같으면 같은 파일이라 영상이 달라도 한 번만 받는다(Landsat SR·ST 공통 파일)
-- 행이 없으면 아카이빙하지 않은 파일이다. 아카이빙을 요청할 때 링크로 찾거나 만들어 image_file과 잇는다
CREATE TABLE archive_file (
    archive_file_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    href            TEXT        NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'ARCHIVING', 'ARCHIVED', 'FAILED')),
    archive_path    TEXT,         -- 아카이브 루트 기준 상대 경로
    size_bytes      BIGINT CHECK (size_bytes >= 0),
    etag            TEXT,         -- 이어받기 If-Range
    extension       VARCHAR(20),
    entries         JSONB,        -- 원본 묶음 내부 목록 [{"path": "...", "size": 123}]
    failure_reason  TEXT,
    archived_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_archive_file_archived CHECK (status <> 'ARCHIVED'
        OR (archive_path IS NOT NULL AND size_bytes IS NOT NULL AND archived_at IS NOT NULL)),
    CONSTRAINT ck_archive_file_failed CHECK (status <> 'FAILED' OR failure_reason IS NOT NULL)
);

CREATE INDEX ix_archive_file_queued ON archive_file (created_at) WHERE status = 'QUEUED';

ALTER TABLE image_file
    ADD COLUMN archive_file_id BIGINT REFERENCES archive_file (archive_file_id) ON DELETE SET NULL;

CREATE INDEX ix_image_file_archive_file ON image_file (archive_file_id);
