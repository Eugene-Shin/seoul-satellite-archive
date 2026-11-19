-- 지표 온도: ECOSTRESS 타일 v003(70 m, 낮·밤 관측)과 MODIS 일별 합성 v061(1 km, Terra·Aqua). NASA 데이터라 출처 표기 의무가 없다
INSERT INTO image_collection (provider, code, title, constellation, product_type, license_name, license_url, credit_text) VALUES
('nasa-cmr', 'C3998139651-LPCLOUD',
 'ECOSTRESS Tiled Land Surface Temperature and Emissivity Instantaneous L2 Global 70 m V003', 'ECOSTRESS', 'L2T_LSTE',
 'Data Use Policy', 'https://earthdata.nasa.gov/earth-observation-data/data-use-policy', NULL),
('nasa-cmr', 'C1748058432-LPCLOUD',
 'MODIS/Terra Land Surface Temperature/Emissivity Daily L3 Global 1km SIN Grid V061', 'MODIS', 'MOD11A1',
 'Data Use Policy', 'https://earthdata.nasa.gov/earth-observation-data/data-use-policy', NULL),
('nasa-cmr', 'C1748046084-LPCLOUD',
 'MODIS/Aqua Land Surface Temperature/Emissivity Daily L3 Global 1km SIN Grid V061', 'MODIS', 'MYD11A1',
 'Data Use Policy', 'https://earthdata.nasa.gov/earth-observation-data/data-use-policy', NULL);

-- ECOSTRESS: 레이어마다 '_{레이어}.tif' 파일 하나. 방출률(Emis1~5)은 열적외 밴드의 측정 중심 파장(JPL ECOSTRESS 장비 사양).
-- LST·방출률·QC·각도는 센서 밴드가 아니라 계산한 값이라 표준 분류(common_name)가 없다
INSERT INTO collect_band_rule (collection_id, file_suffix, band_index, band_name, center_wavelength_um, resolution_meters)
SELECT c.collection_id, '_' || layer.name || '.tif', 1, layer.name, layer.wavelength, 70
FROM image_collection AS c,
     (VALUES ('LST', NULL), ('LST_err', NULL), ('QC', NULL),
             ('Emis1', 8.29), ('Emis2', 8.78), ('Emis3', 9.20), ('Emis4', 10.49), ('Emis5', 12.09),
             ('Emis1_err', NULL), ('Emis2_err', NULL), ('Emis3_err', NULL), ('Emis4_err', NULL), ('Emis5_err', NULL),
             ('EmisWB', NULL), ('height', NULL), ('view_zenith', NULL), ('cloud', NULL), ('water', NULL), ('WST', NULL))
         AS layer (name, wavelength)
WHERE c.provider = 'nasa-cmr' AND c.code = 'C3998139651-LPCLOUD';

-- MODIS: .hdf 하나에 레이어 12개가 이 순서로 들어 있다(MOD11 사용자 가이드 v6.1).
-- Emis_31·Emis_32는 MODIS 31·32번 밴드(11.03·12.02 µm)의 방출률이다
INSERT INTO collect_band_rule (collection_id, file_suffix, band_index, band_name, center_wavelength_um, resolution_meters)
SELECT c.collection_id, '.hdf', layer.band_index, layer.name, layer.wavelength, 1000
FROM image_collection AS c,
     (VALUES (1, 'LST_Day_1km', NULL), (2, 'QC_Day', NULL), (3, 'Day_view_time', NULL), (4, 'Day_view_angl', NULL),
             (5, 'LST_Night_1km', NULL), (6, 'QC_Night', NULL), (7, 'Night_view_time', NULL), (8, 'Night_view_angl', NULL),
             (9, 'Emis_31', 11.03), (10, 'Emis_32', 12.02), (11, 'Clear_day_cov', NULL), (12, 'Clear_night_cov', NULL))
         AS layer (band_index, name, wavelength)
WHERE c.provider = 'nasa-cmr' AND c.code IN ('C1748058432-LPCLOUD', 'C1748046084-LPCLOUD');

-- 서울 지표 온도를 다른 수집 대상처럼 매일 한국 시간 03:00에 수집한다
INSERT INTO collect_target (collection_id, aoi, schedule_cron)
SELECT c.collection_id, p.geom, '0 0 3 * * *'
FROM image_collection AS c, aoi_preset AS p
WHERE c.provider = 'nasa-cmr'
  AND c.code IN ('C3998139651-LPCLOUD', 'C1748058432-LPCLOUD', 'C1748046084-LPCLOUD')
  AND p.code = 'seoul';
