package com.all4land.seoulsatellitearchive.archive;

// 실패 사유로 그대로 남길 메시지
class ArchiveDownloadException extends RuntimeException {

    ArchiveDownloadException(String message) {
        super(message);
    }
}
