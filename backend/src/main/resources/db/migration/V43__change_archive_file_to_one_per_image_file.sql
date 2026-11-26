-- 영상마다 폴더에 파일을 모두 두려고, 링크가 같은 파일(Landsat SR·ST 공통 파일)도 영상 파일마다 따로 받는다
-- 그래서 아카이브 행을 링크 기준이 아니라 영상 파일 1:1로 바꾼다. 행이 없으면 아카이빙하지 않은 파일이다
ALTER TABLE image_file DROP COLUMN archive_file_id;

ALTER TABLE archive_file
    DROP COLUMN archive_file_id,
    DROP COLUMN href,
    ADD COLUMN file_id BIGINT PRIMARY KEY REFERENCES image_file (file_id) ON DELETE CASCADE;
