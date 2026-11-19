-- Sentinel-1 SLC는 ASF가 CMR에 위성마다 컬렉션을 따로 올린다. 1A(C1214470488-ASF)는 V14에 있다. 1B는 2022년에 멈췄다
INSERT INTO image_collection (provider, code, title, constellation, product_type, license_name, license_url, credit_text)
SELECT 'nasa-cmr', s1.code, s1.title, a.constellation, a.product_type, a.license_name, a.license_url, a.credit_text
FROM image_collection AS a,
     (VALUES ('C3470873558-ASF', 'Sentinel-1C Single Look Complex Product'),
             ('C4175278193-ASF', 'Sentinel-1D Single Look Complex Product')) AS s1 (code, title)
WHERE a.provider = 'nasa-cmr' AND a.code = 'C1214470488-ASF';

-- IW 모드 해상도는 ASF가 그래뉼 BEAM_MODE_DESC에 적은 값(거리 5 m, 방위 20 m)이다
INSERT INTO collect_sar_rule (collection_id, instrument_mode, resolution_range_meters, resolution_azimuth_meters)
SELECT collection_id, 'IW', 5, 20
FROM image_collection
WHERE provider = 'nasa-cmr' AND code IN ('C1214470488-ASF', 'C3470873558-ASF', 'C4175278193-ASF');

-- 서울 Sentinel-1을 다른 수집 대상처럼 매일 한국 시간 03:00에 수집한다. 1A는 지금 서울 장면이 없지만 다시 찍으면 들어오도록 둔다
INSERT INTO collect_target (collection_id, aoi, schedule_cron)
SELECT c.collection_id, p.geom, '0 0 3 * * *'
FROM image_collection AS c, aoi_preset AS p
WHERE c.provider = 'nasa-cmr' AND c.code IN ('C1214470488-ASF', 'C3470873558-ASF', 'C4175278193-ASF') AND p.code = 'seoul';
