-- 수집할 때 데이터 파일에 붙일 밴드 규칙. 출처가 밴드 정보를 주지 않는 컬렉션(CMR)만 행이 있다.
-- 데이터 파일 이름이 file_suffix로 끝나면 그 접미사의 행들을 band_index 순서로 붙인다.
-- 파일 하나에 레이어가 여럿이면(MODIS .hdf) 같은 접미사에 행이 여럿이다
CREATE TABLE collect_band_rule (
    rule_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collection_id        BIGINT      NOT NULL REFERENCES image_collection (collection_id) ON DELETE CASCADE,
    file_suffix          VARCHAR(50) NOT NULL,                          -- '.B05.tif', '_LST.tif', '.hdf'
    band_index           SMALLINT    NOT NULL CHECK (band_index > 0),   -- 파일 안 레이어 순서(1부터)
    band_name            VARCHAR(50) NOT NULL,
    common_name          VARCHAR(20) REFERENCES band_common_name (name),
    center_wavelength_um NUMERIC(8,4) CHECK (center_wavelength_um > 0),
    resolution_meters    NUMERIC(8,2) CHECK (resolution_meters > 0),    -- 센서 해상도
    CONSTRAINT uq_collect_band_rule UNIQUE (collection_id, file_suffix, band_index)
);

-- 이름으로 찾던 HLS 밴드(V34)는 파일 이름이 '.{밴드}.tif'로 끝난다
INSERT INTO collect_band_rule (collection_id, file_suffix, band_index, band_name, common_name, center_wavelength_um,
                               resolution_meters)
SELECT collection_id, '.' || band_name || '.tif', 1, band_name, common_name, center_wavelength_um, resolution_meters
FROM collection_band;

DROP TABLE collection_band;
