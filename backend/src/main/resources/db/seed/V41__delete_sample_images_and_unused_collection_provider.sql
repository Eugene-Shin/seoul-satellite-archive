-- V6 샘플 영상과, 그것만 쓰던 컬렉션·제공 기관을 지운다. 영상은 이제 수집기로 들어오고, 검색 테스트는 테스트 fixture를 쓴다.
-- 수집 영상과 직접 등록한 영상은 남기려고 V6 장면을 컬렉션과 장면 ID로 지정한다. 파일·밴드·SAR 행은 함께 지워진다(ON DELETE CASCADE).
DELETE FROM satellite_image AS i
USING image_collection AS c,
      (VALUES ('earth-search', 'sentinel-2-l2a', 'S2A_52SCG_20240903_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2A_52SCG_20250121_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2A_52SCG_20260108_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2A_52SCG_20260428_1_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2B_52SCG_20241217_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2B_52SCG_20241227_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2B_52SCG_20250215_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2B_52SCG_20260111_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2B_52SCG_20260421_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2B_52SDD_20260925_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2C_52SCG_20260307_0_L2A'),
              ('earth-search', 'sentinel-2-l2a', 'S2C_52SDD_20260930_0_L2A'),
              ('nasa-cmr', 'C2021957657-LPCLOUD', 'HLS.L30.T52SCG.2026223T021052.v2.0'),
              ('nasa-cmr', 'C2021957657-LPCLOUD', 'HLS.L30.T52SCG.2026263T021059.v2.0'),
              ('nasa-cmr', 'C1214470488-ASF', 'S1A_IW_SLC__1SDV_20260108T093211_20260108T093238_062674_07DBA1_40A0-SLC'),
              ('nasa-cmr', 'C1214470488-ASF', 'S1A_IW_SLC__1SDV_20260426T093209_20260426T093235_064249_081694_B6EE-SLC'))
          AS seed (provider, code, source_id)
WHERE c.collection_id = i.collection_id
  AND c.provider = seed.provider
  AND c.code = seed.code
  AND i.source_id = seed.source_id
  AND i.collect_target_id IS NULL;

-- 샘플 영상만 쓰던 옛 S2 컬렉션(수집 대상 없음, 수집 불가)과, ASF Search API 대신 CMR로 받기로 해서 쓰지 않는 제공 기관을 지운다.
-- 다른 영상이 남아 있으면 지우지 않는다(FK)
DELETE FROM image_collection AS c
WHERE c.provider = 'earth-search' AND c.code = 'sentinel-2-l2a'
  AND NOT EXISTS (SELECT 1 FROM satellite_image AS i WHERE i.collection_id = c.collection_id);

DELETE FROM provider AS p
WHERE p.code = 'asf'
  AND NOT EXISTS (SELECT 1 FROM image_collection AS c WHERE c.provider = p.code);
