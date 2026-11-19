-- 자주 쓰는 수집 영역. 수집 대상은 만들 때 도형을 복사하므로 프리셋을 바꿔도 기존 대상은 그대로다
CREATE TABLE aoi_preset (
    code   VARCHAR(50)                  PRIMARY KEY,
    name   VARCHAR(100)                 NOT NULL,
    geom   geometry(MultiPolygon, 4326) NOT NULL,
    source VARCHAR(300)                 NOT NULL  -- 화면에 표시할 출처 문구
);
