-- 영상 출처
CREATE TABLE provider (
    code        VARCHAR(30)  PRIMARY KEY,          -- 'earth-search', 'nasa-cmr', 'dx'
    name        VARCHAR(100) NOT NULL,
    is_paid     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
