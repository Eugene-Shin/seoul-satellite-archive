-- 커서 기반 페이지: 기본 정렬(촬영일 최신순 NULLS LAST, id 내림차순)과 같은 방향이어야 정렬 없이 인덱스 순서로 읽는다
DROP INDEX idx_image_acquired;
CREATE INDEX idx_image_acquired_id ON satellite_image (acquired_at DESC NULLS LAST, image_id DESC);
