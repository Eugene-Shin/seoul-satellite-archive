-- 수집기가 출처별로 메타데이터 검색 API와 파일 다운로드 인증 방식을 고른다
ALTER TABLE provider
    ADD COLUMN metadata_search_api VARCHAR(10) NOT NULL DEFAULT 'NONE'
               CHECK (metadata_search_api IN ('STAC', 'CMR', 'ASF_SEARCH', 'NONE')),  -- NONE: 수동 등록
    ADD COLUMN metadata_search_url TEXT,
    ADD COLUMN download_auth       VARCHAR(20) NOT NULL DEFAULT 'NONE'
               CHECK (download_auth IN ('NONE', 'EARTHDATA', 'USGS', 'CDSE_OAUTH', 'AWS_REQUESTER_PAYS')),
    ADD CONSTRAINT ck_provider_metadata_search CHECK ((metadata_search_api = 'NONE') = (metadata_search_url IS NULL));

UPDATE provider
SET metadata_search_api = 'STAC', metadata_search_url = 'https://earth-search.aws.element84.com/v1'
WHERE code = 'earth-search';

UPDATE provider
SET metadata_search_api = 'CMR', metadata_search_url = 'https://cmr.earthdata.nasa.gov/search', download_auth = 'EARTHDATA'
WHERE code = 'nasa-cmr';
