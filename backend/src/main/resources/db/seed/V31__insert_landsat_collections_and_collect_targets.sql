-- Landsat C2 L2는 USGS에서 받는다(Earth Search 판은 Requester Pays). 한 장면이 반사도(SR)와 지표 온도(ST) Item으로 나뉜다.
-- 퍼블릭 도메인이라 출처 표기 의무가 없다. 약관 이름은 USGS가 license로 준 문서의 제목이다
INSERT INTO image_collection (provider, code, title, constellation, product_type, license_name, license_url, credit_text) VALUES
('usgs', 'landsat-c2l2-sr', 'Landsat Collection 2 Level-2 UTM Surface Reflectance (SR) Product', 'LANDSAT', 'L2SR',
 'Landsat Data Policy',
 'https://d9-wret.s3.us-west-2.amazonaws.com/assets/palladium/production/s3fs-public/atoms/files/Landsat_Data_Policy.pdf',
 NULL),
('usgs', 'landsat-c2l2-st', 'Landsat Collection 2 Level-2 UTM Surface Temperature (ST) Product', 'LANDSAT', 'L2ST',
 'Landsat Data Policy',
 'https://d9-wret.s3.us-west-2.amazonaws.com/assets/palladium/production/s3fs-public/atoms/files/Landsat_Data_Policy.pdf',
 NULL);

-- 서울 Landsat SR·ST를 Sentinel-2처럼 매일 한국 시간 03:00에 수집한다
INSERT INTO collect_target (collection_id, aoi, schedule_cron)
SELECT c.collection_id, p.geom, '0 0 3 * * *'
FROM image_collection AS c, aoi_preset AS p
WHERE c.provider = 'usgs' AND c.code IN ('landsat-c2l2-sr', 'landsat-c2l2-st') AND p.code = 'seoul';
