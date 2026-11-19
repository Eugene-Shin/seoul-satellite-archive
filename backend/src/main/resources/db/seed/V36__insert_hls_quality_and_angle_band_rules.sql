-- HLS의 품질(Fmask)·각도 레이어도 파일 안의 레이어라 밴드로 둔다. 규칙에 없는 데이터 파일은 수집할 때 경고가 남는다
INSERT INTO collect_band_rule (collection_id, file_suffix, band_index, band_name)
SELECT c.collection_id, '.' || layer.name || '.tif', 1, layer.name
FROM image_collection AS c,
     (VALUES ('Fmask'), ('SZA'), ('SAA'), ('VZA'), ('VAA')) AS layer (name)
WHERE c.provider = 'nasa-cmr' AND c.code IN ('C2021957657-LPCLOUD', 'C2021957295-LPCLOUD');
