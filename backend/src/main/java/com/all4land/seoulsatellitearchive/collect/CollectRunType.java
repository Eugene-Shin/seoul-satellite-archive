package com.all4land.seoulsatellitearchive.collect;

public enum CollectRunType {
    // 정기 실행. 성공하면 다음 조회 시작점이 된다
    SCHEDULED,
    // 단건 실행. 다음 조회 시작점에 영향을 주지 않는다
    ONE_OFF
}
