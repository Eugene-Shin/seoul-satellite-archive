package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

// 마지막으로 받은 실행의 등록 시각과 id. 이력은 최근 등록이 위로 오므로 다음 페이지는 이보다 앞선 실행이다.
// "등록시각|id"를 Base64URL로 감싼다
public record CollectRunCursor(Instant createdAt, long runId) {

    private static final String DELIMITER = "|";

    // 첫 페이지. 어떤 실행보다 늦은 값이라 모든 실행이 조건을 만족한다
    static final CollectRunCursor FIRST = new CollectRunCursor(Instant.parse("9999-12-31T23:59:59Z"), Long.MAX_VALUE);

    public static CollectRunCursor of(CollectRun last) {
        return new CollectRunCursor(last.getCreatedAt(), last.getId());
    }

    public String encode() {
        String raw = createdAt + DELIMITER + runId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static CollectRunCursor decode(String value) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\" + DELIMITER, -1);
            if (parts.length != 2) {
                throw invalid();
            }
            return new CollectRunCursor(Instant.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw invalid();
        }
    }

    private static BusinessException invalid() {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, "커서가 올바르지 않습니다.");
    }
}
