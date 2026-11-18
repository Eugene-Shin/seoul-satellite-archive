-- 컬렉션마다 수집 대상은 하나다. 여러 지역은 한 대상의 영역(MultiPolygon)에 함께 넣는다.
-- 같은 컬렉션의 대상이 여럿이면 겹치는 영상을 두 번 조회하고, 먼저 만든 대상을 지울 때 그 영상이 수집 영상 표시를 잃는다
ALTER TABLE collect_target ADD CONSTRAINT uq_collect_target_collection UNIQUE (collection_id);
