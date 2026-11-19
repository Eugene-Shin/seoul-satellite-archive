-- 출처가 밴드 정보를 주지 않는 컬렉션(CMR 등)의 밴드 표. 수집기는 파일 이름에서 밴드 이름만 꺼내고 나머지를 여기서 채운다.
-- 같은 밴드 이름도 컬렉션마다 뜻이 다를 수 있다(HLS B05: L30 NIR, S30 레드엣지). STAC처럼 밴드 정보를 주는 출처는 행이 없다
CREATE TABLE collection_band (
    collection_id        BIGINT      NOT NULL REFERENCES image_collection (collection_id) ON DELETE CASCADE,
    band_name            VARCHAR(50) NOT NULL,                         -- 파일 이름의 밴드 이름(B05)
    common_name          VARCHAR(20) REFERENCES band_common_name (name),
    center_wavelength_um NUMERIC(8,4) CHECK (center_wavelength_um > 0),
    resolution_meters    NUMERIC(8,2) CHECK (resolution_meters > 0),   -- 센서 해상도
    PRIMARY KEY (collection_id, band_name)
);
