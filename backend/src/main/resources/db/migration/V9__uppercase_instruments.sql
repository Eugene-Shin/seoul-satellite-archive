-- 센서 이름을 satellite와 같은 규칙(앞뒤 공백 제거 후 대문자)으로 통일한다. 원소 순서는 유지한다
-- NULL 행은 건너뛴다: ARRAY(...)는 빈 배열을 만들어 ck_instruments에 걸린다
UPDATE satellite_image
SET instruments = ARRAY(SELECT upper(btrim(i)) FROM unnest(instruments) WITH ORDINALITY AS t(i, ord) ORDER BY ord)
WHERE instruments IS NOT NULL;
