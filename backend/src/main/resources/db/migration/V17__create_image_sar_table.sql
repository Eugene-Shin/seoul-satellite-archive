-- SAR 영상만 갖는 촬영 기하 정보(1:1). InSAR 쌍·시계열은 같은 상대 궤도·프레임·모드·편파끼리 고른다
-- 값의 의미는 STAC 확장 필드를 따른다(sar:*, sat:relative_orbit, view:incidence_angle). STAC 수집 시 그대로 옮긴다
CREATE TABLE image_sar (
    image_id                  BIGINT PRIMARY KEY REFERENCES satellite_image (image_id) ON DELETE CASCADE,
    relative_orbit            INTEGER CHECK (relative_orbit > 0),           -- 반복 주기 안의 궤도 경로 번호
    frame                     INTEGER CHECK (frame >= 0),                   -- 궤도 경로 위 위치 번호(ASF frame)
    instrument_mode           VARCHAR(10),                                  -- IW, SM 등
    polarizations             VARCHAR[],
    look_direction            VARCHAR(5) CHECK (look_direction IN ('LEFT', 'RIGHT')),
    incidence_angle           NUMERIC(5,2) CHECK (incidence_angle BETWEEN 0 AND 90),  -- 장면 중심, 도
    resolution_range_meters   NUMERIC(8,2) CHECK (resolution_range_meters > 0),       -- 진행 방향에 수직(지상 거리 기준)
    resolution_azimuth_meters NUMERIC(8,2) CHECK (resolution_azimuth_meters > 0),     -- 위성 진행 방향
    CONSTRAINT ck_polarizations CHECK (cardinality(polarizations) > 0
                                       AND polarizations <@ ARRAY['HH', 'HV', 'VH', 'VV']::VARCHAR[])
);

-- 수집기도 같은 필드를 읽는다(ASF CMR: umm.AdditionalAttributes). 출처에 없는 값은 NULL
INSERT INTO image_sar (image_id, relative_orbit, frame, instrument_mode, polarizations, look_direction)
SELECT image_id,
       (max(value) FILTER (WHERE name = 'PATH_NUMBER'))::INTEGER,
       (max(value) FILTER (WHERE name = 'FRAME_NUMBER'))::INTEGER,
       max(value) FILTER (WHERE name = 'BEAM_MODE'),
       string_to_array(max(value) FILTER (WHERE name = 'POLARIZATION'), '+')::VARCHAR[],
       CASE max(value) FILTER (WHERE name = 'LOOK_DIRECTION')
           WHEN 'L' THEN 'LEFT'
           WHEN 'R' THEN 'RIGHT'
       END
FROM (SELECT i.image_id, attribute ->> 'Name' AS name, attribute -> 'Values' ->> 0 AS value
      FROM satellite_image AS i
               JOIN provider AS p ON p.code = i.provider,
           jsonb_array_elements(i.raw_metadata -> 'umm' -> 'AdditionalAttributes') AS attribute
      WHERE p.metadata_search_api = 'CMR'
        AND i.sensor_type = 'SAR') AS attributes
GROUP BY image_id;