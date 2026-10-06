-- 위성 영상
CREATE TABLE satellite_image (
    image_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider            VARCHAR(30)  NOT NULL REFERENCES provider (code),
    source_id           VARCHAR(200) NOT NULL,     -- 출처 내 원본 영상 ID (FK 아님). STAC item id / CMR granule id / DX 제품명
    title               VARCHAR(300) NOT NULL,
    satellite           VARCHAR(50)  NOT NULL,     -- 출처 원본 값을 대문자로. SENTINEL-2A, LANDSAT-9, TDX-1, PAZ-1, WV02 ...
    instruments         TEXT[],                    -- 영상 하나에 여러 개 가능 (Landsat: oli, tirs)
    sensor_type         VARCHAR(10)  NOT NULL CHECK (sensor_type IN ('OPTICAL', 'SAR')),  -- 구름량 필터·광학/SAR 구분
    acquired_at         TIMESTAMPTZ  NOT NULL,
    gsd_meters          NUMERIC(8,2),              -- 대표 공간 해상도 (Ground Sample Distance)
    cloud_cover         NUMERIC(5,2) CHECK (cloud_cover BETWEEN 0 AND 100),  -- 광학만, 0~100 %
    orbit_direction     VARCHAR(10)  CHECK (orbit_direction IN ('ASCENDING', 'DESCENDING')),
    footprint           geometry(MultiPolygon, 4326) NOT NULL,  -- 출처가 준 폴리곤 전부
    url                 TEXT         NOT NULL,     -- 원본 메타데이터 위치(STAC item / CMR concept). DX: 저장 경로
    download_urls       TEXT[]       NOT NULL,     -- 파일 다운로드 링크 (https, s3)
    source_updated_at   TIMESTAMPTZ,
    raw_metadata        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_image_source   UNIQUE (provider, source_id),
    CONSTRAINT ck_cloud_optical  CHECK (sensor_type = 'OPTICAL' OR cloud_cover IS NULL),
    -- 배열은 비어 있거나 NULL·빈 문자열 원소를 가질 수 없다 (instruments는 값이 없으면 배열 대신 NULL)
    CONSTRAINT ck_instruments    CHECK (cardinality(instruments) > 0
                                        AND array_position(instruments, NULL) IS NULL
                                        AND array_position(instruments, '') IS NULL),
    CONSTRAINT ck_download_urls  CHECK (cardinality(download_urls) > 0
                                        AND array_position(download_urls, NULL) IS NULL
                                        AND array_position(download_urls, '') IS NULL)
);

-- 복합 조건은 각 인덱스를 BitmapAnd로 결합해 처리한다
CREATE INDEX idx_image_title_trgm ON satellite_image USING GIN (title gin_trgm_ops);  -- 제목 부분 일치
CREATE INDEX idx_image_acquired   ON satellite_image (acquired_at);                   -- 촬영일시 범위·정렬
CREATE INDEX idx_image_footprint  ON satellite_image USING GIST (footprint);          -- 영역 검색
