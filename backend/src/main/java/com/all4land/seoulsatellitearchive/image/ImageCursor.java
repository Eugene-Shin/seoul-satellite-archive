package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

// 마지막으로 받은 행의 정렬 값과 id. "정렬|촬영일시|구름량|id"를 Base64URL로 감싼다
public record ImageCursor(List<ImageSort> sorts, Instant acquiredAt, BigDecimal cloudCover, long id) {

    private static final String DELIMITER = "|";

    public static ImageCursor of(SatelliteImage last, List<ImageSort> sorts) {
        return new ImageCursor(sorts, last.getAcquiredAt(), last.getCloudCover(), last.getId());
    }

    public String encode() {
        String sortNames = sorts.stream().map(ImageSort::name).collect(Collectors.joining(","));
        String raw = String.join(
                DELIMITER,
                sortNames,
                acquiredAt.toString(),
                cloudCover == null ? "" : cloudCover.toPlainString(),
                Long.toString(id));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static ImageCursor decode(String value, List<ImageSort> requestSorts) {
        ImageCursor cursor;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\" + DELIMITER, -1);
            if (parts.length != 4) {
                throw invalid();
            }
            cursor = new ImageCursor(
                    parts[0].isEmpty()
                            ? List.of()
                            : Arrays.stream(parts[0].split(","))
                                    .map(ImageSort::valueOf)
                                    .toList(),
                    Instant.parse(parts[1]),
                    parts[2].isEmpty() ? null : new BigDecimal(parts[2]),
                    Long.parseLong(parts[3]));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw invalid();
        }
        if (!cursor.sorts().equals(requestSorts)) {
            throw invalid();
        }
        return cursor;
    }

    private static BusinessException invalid() {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, "커서가 올바르지 않습니다.");
    }
}
