-- 검색·표시 테스트용 영상. 제목이 모두 'fx'로 시작해서 keyword=fx로 fixture만 고를 수 있다.
-- 날짜는 KST 하루 경계를 확인하도록 2026-01-08(KST) 안팎에 둔다
--   fx-s2-low     S2 서울, 구름 5,   01-08 11:00 KST, 파일·밴드(썸네일, visual COG, 10~60 m)
--   fx-s2-cloudy  S2 서울, 구름 100, 01-08 00:30 KST
--   fx-s2-busan   S2 부산, 구름 30,  01-09 00:30 KST
--   fx-hls        HLS L30 서울, 구름 10, 01-10, 데이터 30 m와 썸네일
--   fx_s1         S1A SLC 서울, SAR(경로 127, 프레임 120, IW, VV+VH), 원본 zip
INSERT INTO satellite_image (collection_id, source_id, title, satellite, instruments, sensor_type, acquired_at,
                             cloud_cover, orbit_direction, footprint, url, download_urls)
SELECT c.collection_id, f.title, f.title, f.satellite, f.instruments, f.sensor_type, f.acquired_at::timestamptz,
       f.cloud_cover, f.orbit_direction, ST_GeomFromText(f.footprint, 4326), 'https://fx.test/' || f.title,
       ARRAY['https://fx.test/' || f.title || '.tif']
FROM (VALUES ('sentinel-2-l2a', 'fx-s2-low', 'SENTINEL-2A', ARRAY['MSI'], 'OPTICAL', '2026-01-08T02:00:00Z', 5, NULL,
              'MULTIPOLYGON(((126.9 37.5,127.1 37.5,127.1 37.6,126.9 37.6,126.9 37.5)))'),
             ('sentinel-2-l2a', 'fx-s2-cloudy', 'SENTINEL-2B', ARRAY['MSI'], 'OPTICAL', '2026-01-07T15:30:00Z', 100, NULL,
              'MULTIPOLYGON(((126.9 37.5,127.1 37.5,127.1 37.6,126.9 37.6,126.9 37.5)))'),
             ('sentinel-2-l2a', 'fx-s2-busan', 'SENTINEL-2A', ARRAY['MSI'], 'OPTICAL', '2026-01-08T15:30:00Z', 30, NULL,
              'MULTIPOLYGON(((128.95 35.05,129.15 35.05,129.15 35.25,128.95 35.25,128.95 35.05)))'),
             ('C2021957657-LPCLOUD', 'fx-hls', 'LANDSAT-8', ARRAY['OLI'], 'OPTICAL', '2026-01-10T02:00:00Z', 10, NULL,
              'MULTIPOLYGON(((126.9 37.5,127.1 37.5,127.1 37.6,126.9 37.6,126.9 37.5)))'),
             ('C1214470488-ASF', 'fx_s1', 'SENTINEL-1A', ARRAY['C-SAR'], 'SAR', '2026-01-11T09:30:00Z', NULL, 'ASCENDING',
              'MULTIPOLYGON(((126.9 37.5,127.1 37.5,127.1 37.6,126.9 37.6,126.9 37.5)))'))
         AS f (code, title, satellite, instruments, sensor_type, acquired_at, cloud_cover, orbit_direction, footprint)
         JOIN image_collection AS c ON c.code = f.code;

INSERT INTO image_file (image_id, asset_key, role, href, media_type, pixel_spacing_meters)
SELECT i.image_id, f.asset_key, f.role, f.href, f.media_type, f.pixel_spacing
FROM (VALUES ('fx-s2-low', 'red', 'DATA', 'https://fx.test/s2/B04.tif', 'image/tiff; application=geotiff; profile=cloud-optimized', 10),
             ('fx-s2-low', 'coastal', 'DATA', 'https://fx.test/s2/B01.tif', 'image/tiff; application=geotiff; profile=cloud-optimized', 60),
             ('fx-s2-low', 'visual', 'PREVIEW', 'https://fx.test/s2/TCI.tif', 'image/tiff; application=geotiff; profile=cloud-optimized', 10),
             ('fx-s2-low', 'thumbnail', 'THUMBNAIL', 'https://fx.test/s2/preview.jpg', 'image/jpeg', NULL),
             ('fx-s2-low', 'granule_metadata', 'METADATA', 'https://fx.test/s2/metadata.xml', 'application/xml', NULL),
             ('fx-hls', 'fx-hls.B04.tif', 'DATA', 'https://fx.test/hls/fx-hls.B04.tif', NULL, 30),
             ('fx-hls', 'fx-hls.jpg', 'THUMBNAIL', 'https://fx.test/hls/fx-hls.jpg', NULL, NULL),
             ('fx_s1', 'fx_s1.zip', 'ORIGINAL', 'https://fx.test/s1/fx_s1.zip', NULL, NULL))
         AS f (title, asset_key, role, href, media_type, pixel_spacing)
         JOIN satellite_image AS i ON i.source_id = f.title;

INSERT INTO image_file_band (file_id, band_index, band_name, common_name, center_wavelength_um, bandwidth_um,
                             resolution_meters)
SELECT file.file_id, b.band_index, b.band_name, b.common_name, b.center_wavelength, b.bandwidth, b.resolution
FROM (VALUES ('https://fx.test/s2/B04.tif', 1, 'red', 'red', 0.665, 0.038, 10),
             ('https://fx.test/s2/B01.tif', 1, 'coastal', 'coastal', 0.443, 0.027, 60),
             ('https://fx.test/s2/TCI.tif', 1, 'red', 'red', NULL, NULL, NULL),
             ('https://fx.test/s2/TCI.tif', 2, 'green', 'green', NULL, NULL, NULL),
             ('https://fx.test/s2/TCI.tif', 3, 'blue', 'blue', NULL, NULL, NULL))
         AS b (href, band_index, band_name, common_name, center_wavelength, bandwidth, resolution)
         JOIN image_file AS file ON file.href = b.href;

INSERT INTO image_sar (image_id, relative_orbit, frame, instrument_mode, polarizations, look_direction)
SELECT image_id, 127, 120, 'IW', ARRAY['VV', 'VH'], 'RIGHT'
FROM satellite_image
WHERE source_id = 'fx_s1';
