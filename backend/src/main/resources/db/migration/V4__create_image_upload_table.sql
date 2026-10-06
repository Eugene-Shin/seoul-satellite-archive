-- DX 업로드 진행 이력. 압축 해제 전에는 메타데이터를 모르므로 영상 행은 UPLOADED 시점에 만든다
-- 파일 저장 경로는 upload_id로 결정: {저장소}/{upload_id}/{original_file_name}
CREATE TABLE image_upload (
    upload_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    image_id            BIGINT UNIQUE REFERENCES satellite_image (image_id) ON DELETE CASCADE,
    status              VARCHAR(10)   NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'UPLOADING', 'UPLOADED', 'FAILED')),
    original_file_name  VARCHAR(255)  NOT NULL,
    failure_reason      VARCHAR(1000),
    missing_files       TEXT[],                    -- tar 항목 목록 기준 누락 필수 파일
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_upload_image CHECK ((status = 'UPLOADED') = (image_id IS NOT NULL)),
    CONSTRAINT ck_upload_fail  CHECK (status = 'FAILED' OR failure_reason IS NULL)
);

-- 같은 파일 동시 업로드 차단
CREATE UNIQUE INDEX uq_upload_in_progress ON image_upload (original_file_name)
    WHERE status IN ('PENDING', 'UPLOADING');
