-- 매퍼가 없어 수집할 수 없는 컬렉션. 수집 대상 등록·실행이 거절되는지 확인하는 테스트가 쓴다
INSERT INTO image_collection (provider, code, title, constellation, product_type)
VALUES ('earth-search', 'test-uncollectable', '수집할 수 없는 테스트 컬렉션', 'TEST', 'TEST');
