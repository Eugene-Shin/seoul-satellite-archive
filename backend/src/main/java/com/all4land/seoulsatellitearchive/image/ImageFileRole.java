package com.all4land.seoulsatellitearchive.image;

import java.util.regex.Pattern;

public enum ImageFileRole {
    DATA,
    THUMBNAIL,
    PREVIEW,
    METADATA,
    ORIGINAL;

    private static final Pattern METADATA_FILE = Pattern.compile("(?i).*\\.(xml|json)$");
    private static final Pattern ARCHIVE_FILE = Pattern.compile("(?i).*\\.(zip|tar|tar\\.gz|tgz)$");

    // 링크만 받은 파일은 확장자로 정한다. V18 백필과 같은 규칙
    public static ImageFileRole fromHref(String href) {
        if (METADATA_FILE.matcher(href).matches()) {
            return METADATA;
        }
        if (ARCHIVE_FILE.matcher(href).matches()) {
            return ORIGINAL;
        }
        return DATA;
    }
}
