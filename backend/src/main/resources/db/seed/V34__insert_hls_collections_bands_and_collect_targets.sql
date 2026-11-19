-- HLS S30은 Sentinel-2에서 만든 제품이라 Copernicus 출처를 표기한다. L30(C2021957657-LPCLOUD)은 V14에 있다
INSERT INTO image_collection (provider, code, title, constellation, product_type, license_name, license_url, credit_text) VALUES
('nasa-cmr', 'C2021957295-LPCLOUD', 'HLS Sentinel-2 Multi-spectral Instrument Surface Reflectance Daily Global 30m v2.0', 'HLS', 'S30',
 'Data Use Policy', 'https://earthdata.nasa.gov/earth-observation-data/data-use-policy',
 'Contains modified Copernicus Sentinel data');

-- 밴드는 NASA HLS v2 사용자 가이드. 파장은 같은 위성의 다른 출처 값을 따른다(L30: USGS Landsat, S30: Earth Search S2).
-- 해상도는 센서 해상도(HLS 파일은 모두 30 m 격자)
INSERT INTO collection_band (collection_id, band_name, common_name, center_wavelength_um, resolution_meters)
SELECT c.collection_id, b.band_name, b.common_name, b.center_wavelength_um, b.resolution_meters
FROM image_collection AS c
         JOIN (VALUES ('C2021957657-LPCLOUD', 'B01', 'coastal', 0.44, 30),
                      ('C2021957657-LPCLOUD', 'B02', 'blue', 0.48, 30),
                      ('C2021957657-LPCLOUD', 'B03', 'green', 0.56, 30),
                      ('C2021957657-LPCLOUD', 'B04', 'red', 0.65, 30),
                      ('C2021957657-LPCLOUD', 'B05', 'nir08', 0.86, 30),
                      ('C2021957657-LPCLOUD', 'B06', 'swir16', 1.6, 30),
                      ('C2021957657-LPCLOUD', 'B07', 'swir22', 2.2, 30),
                      ('C2021957657-LPCLOUD', 'B09', 'cirrus', 1.37, 30),
                      ('C2021957657-LPCLOUD', 'B10', 'lwir11', 10.9, 100),
                      ('C2021957657-LPCLOUD', 'B11', 'lwir12', 12.0, 100),
                      ('C2021957295-LPCLOUD', 'B01', 'coastal', 0.443, 60),
                      ('C2021957295-LPCLOUD', 'B02', 'blue', 0.49, 10),
                      ('C2021957295-LPCLOUD', 'B03', 'green', 0.56, 10),
                      ('C2021957295-LPCLOUD', 'B04', 'red', 0.665, 10),
                      ('C2021957295-LPCLOUD', 'B05', 'rededge', 0.704, 20),
                      ('C2021957295-LPCLOUD', 'B06', 'rededge', 0.74, 20),
                      ('C2021957295-LPCLOUD', 'B07', 'rededge', 0.783, 20),
                      ('C2021957295-LPCLOUD', 'B08', 'nir', 0.842, 10),
                      ('C2021957295-LPCLOUD', 'B8A', 'nir08', 0.865, 20),
                      ('C2021957295-LPCLOUD', 'B09', 'nir09', 0.945, 60),
                      ('C2021957295-LPCLOUD', 'B10', 'cirrus', 1.375, 60),
                      ('C2021957295-LPCLOUD', 'B11', 'swir16', 1.61, 20),
                      ('C2021957295-LPCLOUD', 'B12', 'swir22', 2.19, 20))
    AS b (code, band_name, common_name, center_wavelength_um, resolution_meters)
              ON b.code = c.code
WHERE c.provider = 'nasa-cmr';

-- 서울 HLS L30·S30을 다른 수집 대상처럼 매일 한국 시간 03:00에 수집한다
INSERT INTO collect_target (collection_id, aoi, schedule_cron)
SELECT c.collection_id, p.geom, '0 0 3 * * *'
FROM image_collection AS c, aoi_preset AS p
WHERE c.provider = 'nasa-cmr' AND c.code IN ('C2021957657-LPCLOUD', 'C2021957295-LPCLOUD') AND p.code = 'seoul';
