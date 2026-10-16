package com.all4land.seoulsatellitearchive.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ImageCursorTest {

    private static final List<ImageSort> SORTS = List.of(ImageSort.CLOUD_COVER_ASC, ImageSort.ACQUIRED_AT_DESC);

    @Test
    void roundTrip() {
        ImageCursor cursor = new ImageCursor(SORTS, Instant.parse("2026-01-08T01:00:00Z"), new BigDecimal("12.50"), 57);

        assertThat(ImageCursor.decode(cursor.encode(), SORTS)).isEqualTo(cursor);
    }

    @Test
    void roundTripWithoutCloudCover() {
        ImageCursor cursor = new ImageCursor(SORTS, Instant.parse("2026-01-08T01:00:00.123Z"), null, 3);

        assertThat(ImageCursor.decode(cursor.encode(), SORTS)).isEqualTo(cursor);
    }

    @Test
    void roundTripWithoutSorts() {
        ImageCursor cursor = new ImageCursor(List.of(), Instant.parse("2026-01-08T01:00:00Z"), null, 3);

        assertThat(ImageCursor.decode(cursor.encode(), List.of())).isEqualTo(cursor);
    }

    @Test
    void encodedValueIsUrlSafe() {
        ImageCursor cursor = new ImageCursor(SORTS, Instant.parse("2026-01-08T01:00:00Z"), null, 255);

        assertThat(cursor.encode()).doesNotContain("+", "/", "=");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "not base64!",
                "ACQUIRED_AT_DESC|2026-01-08T01:00:00Z|57",
                "ACQUIRED_AT_DESC|2026-01-08T01:00:00Z|12.5|57|",
                "ACQUIRED_AT_DESC|2026-01-08|12.5|57",
                "FOO|2026-01-08T01:00:00Z|12.5|57",
                "ACQUIRED_AT_DESC|2026-01-08T01:00:00Z|abc|57"
            })
    void rejectsMalformedCursor(String raw) {
        String value = raw.equals("not base64!") ? raw : encode(raw);

        assertBadRequest(value, List.of(ImageSort.ACQUIRED_AT_DESC));
    }

    @Test
    void rejectsCursorMadeWithOtherSorts() {
        String value = new ImageCursor(
                        List.of(ImageSort.ACQUIRED_AT_DESC), Instant.parse("2026-01-08T01:00:00Z"), null, 57)
                .encode();

        assertBadRequest(value, List.of(ImageSort.CLOUD_COVER_ASC));
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertBadRequest(String value, List<ImageSort> sorts) {
        assertThatThrownBy(() -> ImageCursor.decode(value, sorts))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.BAD_REQUEST);
    }
}
