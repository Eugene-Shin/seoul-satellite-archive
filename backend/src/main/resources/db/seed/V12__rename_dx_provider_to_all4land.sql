-- 자체 보유 데이터 제공 기관을 dx에서 all4land로 바꾼다. code가 PK라 새 행을 만들고 영상을 옮긴 뒤 이전 행을 지운다
INSERT INTO provider (code, name, is_paid, metadata_search_api, metadata_search_url, download_auth)
SELECT 'all4land', 'ALL4LAND', is_paid, metadata_search_api, metadata_search_url, download_auth
FROM provider
WHERE code = 'dx';

UPDATE satellite_image SET provider = 'all4land' WHERE provider = 'dx';

DELETE FROM provider WHERE code = 'dx';
