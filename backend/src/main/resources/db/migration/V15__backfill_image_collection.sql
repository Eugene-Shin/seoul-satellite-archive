-- 출처가 영상에 적어 둔 컬렉션 참조로만 정한다. 수집기도 같은 필드를 읽는다(STAC: collection, CMR: meta.collection-concept-id)
-- 올포랜드 영상은 출처가 컬렉션을 정하지 않아 사람이 고른다(관리 폼, 로컬 시드 R__)
UPDATE satellite_image AS i
SET collection_id = c.collection_id
FROM provider AS p, image_collection AS c
WHERE p.code = i.provider
  AND c.provider = i.provider
  AND c.code = CASE p.metadata_search_api
                   WHEN 'STAC' THEN i.raw_metadata ->> 'collection'
                   WHEN 'CMR'  THEN i.raw_metadata -> 'meta' ->> 'collection-concept-id'
               END;

DO $$
DECLARE
    unresolved TEXT;
BEGIN
    SELECT string_agg(image_id::text, ', ' ORDER BY image_id) INTO unresolved
    FROM satellite_image
    WHERE collection_id IS NULL;
    IF unresolved IS NOT NULL THEN
        RAISE EXCEPTION '컬렉션을 정할 수 없는 영상이 있습니다(image_id: %). 지우거나 collection_id를 직접 넣은 뒤 다시 실행하세요.', unresolved;
    END IF;
END $$;

ALTER TABLE satellite_image ALTER COLUMN collection_id SET NOT NULL;
