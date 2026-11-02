-- Maxar Open Data는 비상업 조건(CC BY-NC)이라 수집 대상에서 제외한다. 영상이 provider를 참조하므로 영상부터 지운다
DELETE FROM satellite_image WHERE provider = 'maxar';
DELETE FROM provider WHERE code = 'maxar';
