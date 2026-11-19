-- 수집하는 S2는 처리 기준이 통일된 Collection-1이다
INSERT INTO image_collection (provider, code, title, constellation, product_type, license_name, license_url, credit_text) VALUES
('earth-search', 'sentinel-2-c1-l2a', 'Sentinel-2 Collection 1 Level-2A', 'SENTINEL-2', 'L2A',
 'proprietary', 'https://sentinels.copernicus.eu/documents/247904/690755/Sentinel_Data_Legal_Notice',
 'Contains modified Copernicus Sentinel data');
