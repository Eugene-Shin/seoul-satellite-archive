package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

// 출처가 준 장면 하나 → 영상 저장 요청과 파일 목록(링크 → 파일 정보)
public interface ItemMapper {

    record Mapped(ImageWriteRequest request, Map<String, ImageFileDetails> files) {}

    Set<String> collections();

    Mapped map(JsonNode item, Long collectionId);
}
