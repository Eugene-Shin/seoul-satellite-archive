-- 영상의 제공 기관은 컬렉션의 제공 기관과 같다. 같은 값을 두 곳에 두지 않는다
ALTER TABLE satellite_image DROP CONSTRAINT uq_image_source;
ALTER TABLE satellite_image DROP COLUMN provider;
ALTER TABLE satellite_image ADD CONSTRAINT uq_image_source UNIQUE (collection_id, source_id);
