-- 값은 V18에서 파일의 픽셀 간격으로, SAR은 V17에서 거리·방위 해상도로 옮겼다
ALTER TABLE satellite_image DROP COLUMN gsd_meters;

-- 영상의 해상도 범위. 파일의 픽셀 간격, 하나도 없으면 센서 해상도(광학은 밴드, SAR은 거리·방위)로 대체
-- 검색 필터(범위가 겹치는 영상)와 응답이 같이 읽는다
CREATE VIEW image_resolution AS
WITH spacing AS (SELECT image_id, pixel_spacing_meters AS meters
                 FROM image_file
                 WHERE pixel_spacing_meters IS NOT NULL)
SELECT image_id, min(meters) AS min_meters, max(meters) AS max_meters
FROM (SELECT image_id, meters
      FROM spacing
      UNION ALL
      SELECT f.image_id, b.resolution_meters
      FROM image_file_band AS b
               JOIN image_file AS f ON f.file_id = b.file_id
      WHERE b.resolution_meters IS NOT NULL
        AND f.image_id NOT IN (SELECT image_id FROM spacing)
      UNION ALL
      SELECT s.image_id, resolution.meters
      FROM image_sar AS s,
           LATERAL (VALUES (s.resolution_range_meters), (s.resolution_azimuth_meters)) AS resolution (meters)
      WHERE resolution.meters IS NOT NULL
        AND s.image_id NOT IN (SELECT image_id FROM spacing)) AS resolution
GROUP BY image_id;
