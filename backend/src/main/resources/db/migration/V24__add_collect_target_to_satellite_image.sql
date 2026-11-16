-- 수집기가 만든 영상만 값이 있다. 대상을 지워도 영상은 남긴다
ALTER TABLE satellite_image
    ADD COLUMN collect_target_id BIGINT REFERENCES collect_target (target_id) ON DELETE SET NULL;
