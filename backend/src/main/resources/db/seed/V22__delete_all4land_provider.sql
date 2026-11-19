-- 올포랜드 제공 기관과 그 컬렉션을 지운다. 이 컬렉션의 영상은 로컬 전용 시드에만 있어서 여기서 지우지 않는다
DELETE FROM image_collection WHERE provider = 'all4land';
DELETE FROM provider WHERE code = 'all4land';
