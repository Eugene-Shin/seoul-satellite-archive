package com.all4land.seoulsatellitearchive.collect;

public enum CollectRunStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED;

    public boolean isFinished() {
        return this == SUCCEEDED || this == FAILED || this == CANCELED;
    }
}
