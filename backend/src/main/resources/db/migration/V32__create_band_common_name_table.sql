-- 밴드 분류(common_name) 사전. STAC eo 확장 v2.0.0의 표준 이름이다. CHECK 대신 표로 두어 밴드를 쓰는 표가 같은 목록을 참조한다
CREATE TABLE band_common_name (
    name VARCHAR(20) PRIMARY KEY
);

INSERT INTO band_common_name (name) VALUES
('pan'), ('coastal'), ('blue'), ('green'), ('green05'), ('yellow'), ('red'),
('rededge'), ('rededge071'), ('rededge075'), ('rededge078'),
('nir'), ('nir08'), ('nir09'), ('cirrus'), ('swir16'), ('swir22'), ('lwir'), ('lwir11'), ('lwir12');

ALTER TABLE image_file_band
    DROP CONSTRAINT image_file_band_common_name_check,
    ADD CONSTRAINT fk_image_file_band_common_name FOREIGN KEY (common_name) REFERENCES band_common_name (name);
