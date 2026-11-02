-- 제품 유형은 컬렉션 정의의 일부다(같은 컬렉션의 영상은 모두 같은 유형). 처리 단계(L1/L2)보다 세밀한 구분
ALTER TABLE image_collection ADD COLUMN product_type VARCHAR(20);

UPDATE image_collection
SET product_type = CASE code
                       WHEN 'sentinel-2-l2a'      THEN 'L2A'
                       WHEN 'C2021957657-LPCLOUD' THEN 'L30'
                       WHEN 'C1214470488-ASF'     THEN 'SLC'
                       WHEN 'TERRASAR-X_SSC'      THEN 'SSC'
                       WHEN 'PAZ_SSC'             THEN 'SSC'
                   END;

ALTER TABLE image_collection ALTER COLUMN product_type SET NOT NULL;

-- 촬영 종료 일시. 출처가 준 값 그대로이며, 한 시각만 주는 출처(STAC datetime)는 NULL
ALTER TABLE satellite_image
    ADD COLUMN acquired_end_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_acquired_range CHECK (acquired_end_at IS NULL OR acquired_end_at >= acquired_at);

-- 수집기도 같은 필드를 읽는다(STAC: end_datetime, CMR: umm.TemporalExtent.RangeDateTime.EndingDateTime)
UPDATE satellite_image AS i
SET acquired_end_at = CASE p.metadata_search_api
                          WHEN 'STAC' THEN (i.raw_metadata ->> 'end_datetime')::timestamptz
                          WHEN 'CMR'  THEN (i.raw_metadata -> 'umm' -> 'TemporalExtent' -> 'RangeDateTime'
                                                ->> 'EndingDateTime')::timestamptz
                      END
FROM provider AS p
WHERE p.code = i.provider;
