-- 같은 정의(위성 계열·처리 단계·처리 방식·출처)를 따르는 제품의 묶음. STAC Collection, CMR collection에 해당한다
-- provider는 이 컬렉션을 정의한 출처다. 영상의 provider(어디서 받았나)와 다를 수 있다(올포랜드가 보유한 다른 출처 제품)
CREATE TABLE image_collection (
    collection_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider        VARCHAR(30)  NOT NULL REFERENCES provider (code),
    code            VARCHAR(100) NOT NULL,     -- 출처가 정한 컬렉션 ID(STAC collection id, CMR concept id). 올포랜드는 직접 정한다
    title           VARCHAR(300) NOT NULL,
    constellation   VARCHAR(50)  NOT NULL,     -- 아카이브 폴더 최상위. 위성이 섞인 제품군(HLS)은 제품군 이름
    license_name    VARCHAR(300),              -- 출처가 준 약관 이름
    license_url     TEXT,
    credit_text     VARCHAR(300),              -- 재배포·게시할 때 적어야 하는 출처 표기 문구. 의무가 없으면 NULL
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_collection UNIQUE (provider, code)
);

-- 백필 후 NOT NULL로 바꾼다(V15)
ALTER TABLE satellite_image ADD COLUMN collection_id BIGINT REFERENCES image_collection (collection_id);
