-- 출처가 주는 파일 링크. 픽셀 간격과 밴드 해상도는 파일·밴드마다 다르므로 여기에 둔다
CREATE TABLE image_file (
    file_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    image_id             BIGINT       NOT NULL REFERENCES satellite_image (image_id) ON DELETE CASCADE,
    asset_key            VARCHAR(200) NOT NULL,  -- STAC 에셋 키. 그 외 출처는 파일 이름
    role                 VARCHAR(20)  NOT NULL CHECK (role IN ('DATA', 'THUMBNAIL', 'PREVIEW', 'METADATA', 'ORIGINAL')),
    href                 TEXT         NOT NULL,
    media_type           VARCHAR(200),
    pixel_spacing_meters NUMERIC(8,2) CHECK (pixel_spacing_meters > 0),  -- 방향마다 다르면 거친 값
    -- 같은 파일을 가리키는 에셋이 여럿이면 한 행만 둔다. 썸네일은 출처에 따라 여러 개일 수 있다
    CONSTRAINT uq_image_file_href UNIQUE (image_id, href)
);

-- 값의 의미는 STAC eo:bands를 따른다. common_name은 eo 확장의 표준 이름만, 해당 없으면 NULL
CREATE TABLE image_file_band (
    file_id              BIGINT   NOT NULL REFERENCES image_file (file_id) ON DELETE CASCADE,
    band_index           SMALLINT NOT NULL CHECK (band_index > 0),  -- 1부터. 래스터 밴드 번호와 같은 순서
    band_name            VARCHAR(50),                               -- 출처 이름 그대로 (B04, red 등). 없을 수 있다
    common_name          VARCHAR(20) CHECK (common_name IN (
                             'pan', 'coastal', 'blue', 'green', 'green05', 'yellow', 'red',
                             'rededge', 'rededge071', 'rededge075', 'rededge078',
                             'nir', 'nir08', 'nir09', 'cirrus', 'swir16', 'swir22', 'lwir', 'lwir11', 'lwir12')),
    center_wavelength_um NUMERIC(8,4) CHECK (center_wavelength_um > 0),
    bandwidth_um         NUMERIC(8,4) CHECK (bandwidth_um > 0),       -- 반치폭(FWHM)
    resolution_meters    NUMERIC(8,2) CHECK (resolution_meters > 0),  -- 센서 해상도. 픽셀 간격과 다를 수 있다(Landsat 열적외)
    PRIMARY KEY (file_id, band_index)
);

-- 수집기도 같은 규칙을 쓴다
-- STAC: 에셋. 픽셀 간격은 proj:transform [a, b, c, d, e, f]의 가로 √(a²+d²)·세로 √(b²+e²)(회전 격자 포함)
-- proj:transform은 좌표계 단위라 미터 좌표계일 때만 쓴다
INSERT INTO image_file (image_id, asset_key, role, href, media_type, pixel_spacing_meters)
SELECT i.image_id,
       asset.key,
       CASE
           WHEN asset.value -> 'roles' ? 'thumbnail' THEN 'THUMBNAIL'
           WHEN asset.value -> 'roles' ?| ARRAY['overview', 'visual'] THEN 'PREVIEW'
           WHEN asset.value -> 'roles' ? 'metadata' THEN 'METADATA'
           ELSE 'DATA'
       END,
       asset.value ->> 'href',
       asset.value ->> 'type',
       CASE
           WHEN EXISTS (SELECT 1
                        FROM spatial_ref_sys AS s
                        WHERE s.srid = (i.raw_metadata -> 'properties' ->> 'proj:epsg')::INTEGER
                          AND s.proj4text LIKE '%+units=m %')
               THEN greatest(sqrt((t ->> 0)::NUMERIC ^ 2 + (t ->> 3)::NUMERIC ^ 2),
                             sqrt((t ->> 1)::NUMERIC ^ 2 + (t ->> 4)::NUMERIC ^ 2))
       END
FROM satellite_image AS i
         JOIN provider AS p ON p.code = i.provider,
     jsonb_each(i.raw_metadata -> 'assets') AS asset,
     LATERAL (SELECT asset.value -> 'proj:transform' AS t) AS transform
WHERE p.metadata_search_api = 'STAC'
ON CONFLICT (image_id, href) DO NOTHING;

INSERT INTO image_file_band (file_id, band_index, band_name, common_name, center_wavelength_um, bandwidth_um,
                             resolution_meters)
SELECT f.file_id,
       band.index,
       band.value ->> 'name',
       band.value ->> 'common_name',
       (band.value ->> 'center_wavelength')::NUMERIC,
       (band.value ->> 'full_width_half_max')::NUMERIC,
       coalesce((band.value ->> 'gsd')::NUMERIC, (i.raw_metadata -> 'assets' -> f.asset_key ->> 'gsd')::NUMERIC)
FROM image_file AS f
         JOIN satellite_image AS i ON i.image_id = f.image_id
         JOIN provider AS p ON p.code = i.provider,
     jsonb_array_elements(i.raw_metadata -> 'assets' -> f.asset_key -> 'eo:bands') WITH ORDINALITY AS band (value, index)
WHERE p.metadata_search_api = 'STAC';

-- CMR: RelatedUrls의 https 링크만(s3:// 직접 접근은 같은 파일을 같은 리전에서 읽는 경로, 안내 페이지는 파일이 아님)
-- OPeNDAP·WMS 같은 서비스 주소(Subtype)도 파일이 아니라 뺀다
-- HLS는 파일별 픽셀 간격이 없어서 그래뉼의 SPATIAL_RESOLUTION을 DATA 파일에 쓴다(모든 밴드가 같은 격자)
INSERT INTO image_file (image_id, asset_key, role, href, media_type, pixel_spacing_meters)
SELECT i.image_id,
       regexp_replace(url.value ->> 'URL', '^.*/', ''),
       url.role,
       url.value ->> 'URL',
       url.value ->> 'MimeType',
       CASE url.role
           WHEN 'DATA' THEN (SELECT (attribute -> 'Values' ->> 0)::NUMERIC
                             FROM jsonb_array_elements(i.raw_metadata -> 'umm' -> 'AdditionalAttributes') AS attribute
                             WHERE attribute ->> 'Name' = 'SPATIAL_RESOLUTION')
       END
FROM satellite_image AS i
         JOIN provider AS p ON p.code = i.provider,
     LATERAL (SELECT related.value,
                     CASE
                         WHEN related.value ->> 'Type' = 'GET DATA'
                             AND related.value ->> 'URL' ~* '\.(zip|tar|tar\.gz|tgz)$' THEN 'ORIGINAL'
                         WHEN related.value ->> 'Type' = 'GET DATA' THEN 'DATA'
                         WHEN related.value ->> 'Type' = 'GET RELATED VISUALIZATION' THEN 'THUMBNAIL'
                         WHEN related.value ->> 'Type' = 'VIEW RELATED INFORMATION'
                             AND related.value ->> 'URL' ~* '\.(xml|json)$' THEN 'METADATA'
                     END AS role
              FROM jsonb_array_elements(i.raw_metadata -> 'umm' -> 'RelatedUrls') AS related) AS url
WHERE p.metadata_search_api = 'CMR'
  AND url.value ->> 'URL' LIKE 'https://%'
  AND url.role IS NOT NULL
  AND coalesce(url.value ->> 'Subtype', '') !~* 'OPENDAP|THREDDS|WMS|WMTS|WCS|WFS|SUBSET|SERVICE|API'
ON CONFLICT (image_id, href) DO NOTHING;

-- 직접 등록(올포랜드): 다운로드 URL의 확장자로 역할을 정한다
INSERT INTO image_file (image_id, asset_key, role, href)
SELECT i.image_id,
       regexp_replace(download_url, '^.*/', ''),
       CASE
           WHEN download_url ~* '\.(xml|json)$' THEN 'METADATA'
           WHEN download_url ~* '\.(zip|tar|tar\.gz|tgz)$' THEN 'ORIGINAL'
           ELSE 'DATA'
       END,
       download_url
FROM satellite_image AS i
         JOIN provider AS p ON p.code = i.provider,
     unnest(i.download_urls) AS download_url
WHERE p.metadata_search_api = 'NONE'
ON CONFLICT (image_id, href) DO NOTHING;
