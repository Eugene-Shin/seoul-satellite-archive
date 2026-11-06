-- Earth Search가 주는 sentinel.esa.int 주소는 접속되지 않는다. S1 컬렉션(ASF 제공)과 같은 Copernicus 주소로 바꾼다
UPDATE image_collection
SET license_url = 'https://sentinels.copernicus.eu/documents/247904/690755/Sentinel_Data_Legal_Notice'
WHERE provider = 'earth-search' AND code = 'sentinel-2-l2a';
