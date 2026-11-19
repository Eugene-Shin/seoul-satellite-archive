-- IW 방위 해상도를 ESA 사양(IW SLC 약 22 m)으로 고친다. V39의 20 m는 ASF가 반올림해 적은 값이다.
-- 거리 해상도 5 m는 ESA 경사거리 해상도 2.7~3.5 m를 지상 거리로 바꾼 값과 같아 그대로 둔다(image_sar는 지상 거리 기준)
UPDATE collect_sar_rule
SET resolution_azimuth_meters = 22
WHERE instrument_mode = 'IW'
  AND collection_id IN (SELECT collection_id FROM image_collection
                        WHERE provider = 'nasa-cmr' AND code IN ('C1214470488-ASF', 'C3470873558-ASF', 'C4175278193-ASF'));

-- 이미 이 규칙으로 저장된 수집 영상도 고친다(출처가 해상도를 주지 않아 규칙 값이 들어간 행)
UPDATE image_sar AS s
SET resolution_azimuth_meters = 22
FROM satellite_image AS i
         JOIN image_collection AS c ON c.collection_id = i.collection_id
WHERE s.image_id = i.image_id
  AND i.collect_target_id IS NOT NULL
  AND c.provider = 'nasa-cmr' AND c.code IN ('C1214470488-ASF', 'C3470873558-ASF', 'C4175278193-ASF')
  AND s.instrument_mode = 'IW'
  AND s.resolution_azimuth_meters = 20;
