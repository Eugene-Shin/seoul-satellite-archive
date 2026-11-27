package com.all4land.seoulsatellitearchive.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// 출처 파일을 {경로}.part에 받고 다 받으면 이름을 바꾼다. 끊기면 .part를 남겨 두고 다음에 Range + If-Range로 이어받는다.
// 받는 도중 60초 동안 데이터가 오지 않으면 실패하도록 HttpURLConnection(SimpleClientHttpRequestFactory)을 쓴다.
// JDK HttpClient의 시간 제한은 응답 헤더까지만 걸린다. 리다이렉트는 따라간다
@Component
class ArchiveDownloader {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    // 경로에 확장자가 없을 때 응답 Content-Type으로 정한다
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/tiff", "tif",
            "image/jpeg", "jpg",
            "image/png", "png",
            "application/zip", "zip",
            "application/xml", "xml",
            "text/xml", "xml",
            "application/json", "json",
            "application/x-hdf", "hdf",
            "application/gzip", "gz");

    private final RestClient restClient;

    ArchiveDownloader() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    record Downloaded(Path path, String extension, long sizeBytes) {}

    // onEtag: 응답의 ETag를 받기 전에 저장해 두어야 끊겼을 때 이어받을 수 있다
    Downloaded download(String href, Path target, String etag, Consumer<String> onEtag) {
        Path part = target.resolveSibling(target.getFileName() + ".part");
        long received = sizeOf(part);
        return restClient
                .get()
                .uri(URI.create(href))
                .headers(headers -> {
                    if (received > 0 && etag != null) {
                        headers.set(HttpHeaders.RANGE, "bytes=" + received + "-");
                        headers.set(HttpHeaders.IF_RANGE, etag);
                    }
                })
                .exchange((request, response) -> save(response, target, part, received, onEtag));
    }

    // 206이면 이어 쓰고, 200이면(처음이거나 그사이 파일이 바뀌었으면) 처음부터 쓴다
    private static Downloaded save(
            ClientHttpResponse response, Path target, Path part, long received, Consumer<String> onEtag)
            throws IOException {
        int status = response.getStatusCode().value();
        if (status == 416) {
            Files.deleteIfExists(part);
        }
        if (status != 200 && status != 206) {
            throw new ArchiveDownloadException("출처가 " + status + "로 응답했습니다.");
        }
        HttpHeaders headers = response.getHeaders();
        if (headers.getETag() != null) {
            onEtag.accept(headers.getETag());
        }
        boolean append = status == 206;
        Files.createDirectories(part.getParent());
        try (InputStream in = response.getBody();
                OutputStream out = Files.newOutputStream(
                        part,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
        long size = Files.size(part);
        long length = headers.getContentLength();
        long expected = length < 0 ? -1 : (append ? received : 0) + length;
        if (expected >= 0 && size != expected) {
            throw new ArchiveDownloadException("받은 크기(" + size + ")가 출처 크기(" + expected + ")와 다릅니다.");
        }
        String extension = ArchivePaths.extension(target.getFileName().toString());
        Path saved = target;
        if (extension.isEmpty()) {
            extension = headerExtension(headers);
            if (!extension.isEmpty()) {
                saved = target.resolveSibling(target.getFileName() + "." + extension);
            }
        }
        Files.move(part, saved, StandardCopyOption.REPLACE_EXISTING);
        return new Downloaded(saved, extension, size);
    }

    // Content-Disposition 파일 이름이 먼저, 없으면 Content-Type. 못 찾으면 빈 문자열
    private static String headerExtension(HttpHeaders headers) {
        String fileName = headers.getContentDisposition().getFilename();
        if (fileName != null && !ArchivePaths.extension(fileName).isEmpty()) {
            return ArchivePaths.extension(fileName);
        }
        MediaType type = headers.getContentType();
        return type == null ? "" : EXTENSIONS.getOrDefault(type.getType() + "/" + type.getSubtype(), "");
    }

    private static long sizeOf(Path path) {
        try {
            return Files.exists(path) ? Files.size(path) : 0;
        } catch (IOException e) {
            return 0;
        }
    }
}
