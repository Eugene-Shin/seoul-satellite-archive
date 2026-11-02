-- 기존 시드 영상이 속한 컬렉션. 약관 이름·주소는 출처 컬렉션 메타데이터 원본 값(2026-11-02 확인)
-- Copernicus 표기 문구의 연도는 화면에 표시할 때 촬영 연도를 붙인다
INSERT INTO image_collection (provider, code, title, constellation, license_name, license_url, credit_text) VALUES
('earth-search', 'sentinel-2-l2a', 'Sentinel-2 Level-2A', 'SENTINEL-2',
 'proprietary', 'https://sentinel.esa.int/documents/247904/690755/Sentinel_Data_Legal_Notice',
 'Contains modified Copernicus Sentinel data'),
('nasa-cmr', 'C2021957657-LPCLOUD',
 'HLS Landsat Operational Land Imager Surface Reflectance and TOA Brightness Daily Global 30m v2.0', 'HLS',
 'Data Use Policy', 'https://earthdata.nasa.gov/earth-observation-data/data-use-policy',
 NULL),
('nasa-cmr', 'C1214470488-ASF', 'Sentinel-1A Single Look Complex Product', 'SENTINEL-1',
 'Legal notice on the use of Copernicus Sentinel Data and Service Information',
 'https://sentinels.copernicus.eu/documents/247904/690755/Sentinel_Data_Legal_Notice',
 'Contains modified Copernicus Sentinel data'),
-- 올포랜드 보유 상업 제품은 출처가 컬렉션을 정하지 않아 제품군·계약 단위로 직접 정한다. 약관은 계약 조건이라 비워 둔다
('all4land', 'TERRASAR-X_SSC', 'TerraSAR-X / TanDEM-X SSC', 'TERRASAR-X', NULL, NULL, NULL),
('all4land', 'PAZ_SSC', 'PAZ SSC', 'PAZ', NULL, NULL, NULL);
