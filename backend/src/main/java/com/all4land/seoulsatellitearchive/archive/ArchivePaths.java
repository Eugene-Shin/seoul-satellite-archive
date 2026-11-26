package com.all4land.seoulsatellitearchive.archive;

import com.all4land.seoulsatellitearchive.collect.ItemMapper;
import com.all4land.seoulsatellitearchive.collect.ItemMappers;
import com.all4land.seoulsatellitearchive.image.ImageFile;
import com.all4land.seoulsatellitearchive.image.ImageFileBand;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

// 아카이브 상대 경로(13-9): {constellation}/{product}/{grid}/{촬영 UTC}_{source_id}/{파일 이름}.
// 경로는 한 번 만들어 archive_path로 저장하고 다시 계산하지 않는다. 대소문자는 바꾸지 않는다
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArchivePaths {

    // 루트(약 25자)를 더해도 Windows 260자 안에 들어가게
    static final int MAX_LENGTH = 200;

    private static final DateTimeFormatter ACQUIRED =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC);

    // 우리가 만드는 파일. 출처 파일이 이 이름을 쓰지 않게 비워 둔다
    private static final List<String> RESERVED = List.of("preview.webp", "metadata.json");

    // relativeOrbit·orbitDirection은 SAR만, raw는 저장한 raw_metadata
    public record Scene(
            String constellation,
            String productType,
            String collectionCode,
            String sourceId,
            Instant acquiredAt,
            String orbitDirection,
            Integer relativeOrbit,
            JsonNode raw) {}

    // 파일 → 상대 경로. 이름이 겹치면 목록 순서대로 _2, _3을 붙인다
    public static Map<ImageFile, String> paths(Scene scene, List<ImageFile> files) {
        String folder = String.join(
                "/",
                segment(scene.constellation()),
                segment(scene.productType()),
                segment(grid(scene)),
                segment(ACQUIRED.format(scene.acquiredAt()) + "_" + scene.sourceId()));
        Set<String> used = new HashSet<>(RESERVED);
        Map<ImageFile, String> paths = new LinkedHashMap<>();
        for (ImageFile file : files) {
            String path = folder + "/" + unique(segment(fileName(scene.sourceId(), file)), used);
            if (path.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("아카이브 경로가 " + MAX_LENGTH + "자를 넘습니다: " + path);
            }
            paths.put(file, path);
        }
        return paths;
    }

    // SAR은 방향-상대궤도(ASC-127), 광학은 매퍼가 raw_metadata에서 읽는다. 둘 중 하나라도 없으면 _
    private static String grid(Scene scene) {
        if (scene.relativeOrbit() != null && scene.orbitDirection() != null) {
            String direction = scene.orbitDirection().startsWith("ASC") ? "ASC" : "DSC";
            return "%s-%03d".formatted(direction, scene.relativeOrbit());
        }
        String grid = ItemMappers.find(scene.collectionCode())
                .map(mapper -> mapperGrid(mapper, scene.raw()))
                .orElse(null);
        return grid == null || grid.isBlank() ? "_" : grid;
    }

    private static String mapperGrid(ItemMapper mapper, JsonNode raw) {
        return raw == null ? null : mapper.grid(raw);
    }

    // 원본 묶음 → 밴드 하나짜리 파일 → 출처 이름(에셋 키, 앞의 장면 ID는 뗀다) 순서
    private static String fileName(String sourceId, ImageFile file) {
        String extension = extension(file.getHref());
        String dotExtension = extension.isEmpty() ? "" : "." + extension;
        if (file.getRole() == ImageFileRole.ORIGINAL) {
            return "original" + dotExtension;
        }
        List<ImageFileBand> bands = file.getBands();
        if (bands.size() == 1 && bands.getFirst().getBandName() != null) {
            ImageFileBand band = bands.getFirst();
            String commonName = band.getCommonName() == null ? "" : "_" + band.getCommonName();
            return band.getBandName() + commonName + dotExtension;
        }
        String name = withoutScenePrefix(file.getAssetKey(), sourceId, extension);
        return name.endsWith(dotExtension) ? name : name + dotExtension;
    }

    // ECOv003_..._01_LST.jpeg → LST.jpeg. 확장자만 남으면(HLS 썸네일, MODIS .hdf) 그대로 둔다
    private static String withoutScenePrefix(String name, String sourceId, String extension) {
        if (sourceId == null
                || name.length() <= sourceId.length() + 1
                || !name.startsWith(sourceId)
                || "_.".indexOf(name.charAt(sourceId.length())) < 0) {
            return name;
        }
        String rest = name.substring(sourceId.length() + 1);
        return rest.equals(extension) ? name : rest;
    }

    // 링크의 파일 이름에서. tar.gz는 한 확장자로 본다
    private static String extension(String href) {
        String fileName = href.replaceFirst("[?#].*$", "");
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".tar.gz")) {
            return fileName.substring(fileName.length() - "tar.gz".length());
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    // Windows는 대소문자만 다른 이름을 같은 파일로 보므로 소문자로 비교한다
    private static String unique(String name, Set<String> used) {
        String candidate = name;
        int dot = name.indexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        String rest = dot < 0 ? "" : name.substring(dot);
        for (int n = 2; !used.add(candidate.toLowerCase(Locale.ROOT)); n++) {
            candidate = base + "_" + n + rest;
        }
        return candidate;
    }

    // 영문·숫자·-·_·.만 둔다. 상위 폴더(..)와 숨김 이름(.으로 시작)은 막는다
    private static String segment(String value) {
        String safe = value.replaceAll("[^A-Za-z0-9._-]", "_").replace("..", "__");
        return safe.startsWith(".") ? "_" + safe.substring(1) : safe;
    }
}
