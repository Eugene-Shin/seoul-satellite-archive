-- 수집할 때 SAR 영상(image_sar)에 채울 해상도 규칙. 출처가 해상도를 값으로 주지 않는 컬렉션만 행이 있다.
-- 같은 촬영 모드 이름도 위성마다 해상도가 다를 수 있어 컬렉션별로 둔다
CREATE TABLE collect_sar_rule (
    rule_id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collection_id             BIGINT       NOT NULL REFERENCES image_collection (collection_id) ON DELETE CASCADE,
    instrument_mode           VARCHAR(10)  NOT NULL,                                        -- IW, EW, SM
    resolution_range_meters   NUMERIC(8,2) CHECK (resolution_range_meters > 0),             -- 진행 방향에 수직
    resolution_azimuth_meters NUMERIC(8,2) CHECK (resolution_azimuth_meters > 0),           -- 위성 진행 방향
    CONSTRAINT uq_collect_sar_rule UNIQUE (collection_id, instrument_mode)
);
