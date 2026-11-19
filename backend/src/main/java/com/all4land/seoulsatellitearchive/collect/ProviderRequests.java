package com.all4land.seoulsatellitearchive.collect;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

// 출처 클라이언트가 함께 쓰는 요청 규칙: 시간 제한과, 429·5xx·연결 오류의 재시도(1·2·4초, Retry-After 우선)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ProviderRequests {

    private static final int MAX_RETRIES = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    static <T> T withRetry(Supplier<T> request) {
        for (int attempt = 0; ; attempt++) {
            try {
                return request.get();
            } catch (HttpStatusCodeException e) {
                if (!isRetryable(e.getStatusCode()) || attempt == MAX_RETRIES) {
                    throw e;
                }
                sleep(retryAfter(e).orElse(backoff(attempt)));
            } catch (ResourceAccessException e) {
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
                sleep(backoff(attempt));
            }
        }
    }

    private static boolean isRetryable(HttpStatusCode status) {
        return status.value() == 429 || status.is5xxServerError();
    }

    // 초 단위 값만 따른다. 날짜 형식이면 기본 대기 시간을 쓴다
    private static Optional<Duration> retryAfter(HttpStatusCodeException e) {
        String value =
                e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
        if (value == null || !value.matches("\\d+")) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(Long.parseLong(value)));
    }

    private static Duration backoff(int attempt) {
        return Duration.ofSeconds(1L << attempt);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("출처 요청 재시도 대기 중 중단되었습니다.", e);
        }
    }
}
