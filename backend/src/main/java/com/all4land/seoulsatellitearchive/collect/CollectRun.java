package com.all4land.seoulsatellitearchive.collect;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private CollectTarget target;

    @Enumerated(EnumType.STRING)
    private CollectRunType runType;

    @Enumerated(EnumType.STRING)
    private CollectRunStatus status;

    private Instant windowFrom;

    private Instant windowTo;

    private int found;

    private int inserted;

    private int updated;

    private int skipped;

    private String failureReason;

    private Instant createdAt;

    private Instant startedAt;

    private Instant cancelRequestedAt;

    private Instant finishedAt;

    @ElementCollection
    @CollectionTable(name = "collect_run_skip", joinColumns = @JoinColumn(name = "run_id"))
    private List<CollectRunSkip> skips = new ArrayList<>();

    // 조회 구간이 없으면 워커가 시작할 때 정한다
    public CollectRun(
            CollectTarget target, CollectRunType runType, Instant windowFrom, Instant windowTo, Instant createdAt) {
        this.target = target;
        this.runType = runType;
        this.status = CollectRunStatus.QUEUED;
        this.windowFrom = windowFrom;
        this.windowTo = windowTo;
        this.createdAt = createdAt;
    }

    public void start(Instant windowFrom, Instant windowTo, Instant startedAt) {
        this.status = CollectRunStatus.RUNNING;
        this.windowFrom = windowFrom;
        this.windowTo = windowTo;
        this.startedAt = startedAt;
    }

    public void succeed(int found, int inserted, int updated, List<CollectRunSkip> skips, Instant finishedAt) {
        finish(CollectRunStatus.SUCCEEDED, found, inserted, updated, skips, finishedAt);
    }

    // 멈추기 전까지 저장한 영상은 남으므로 건수도 남긴다
    public void cancel(int found, int inserted, int updated, List<CollectRunSkip> skips, Instant finishedAt) {
        finish(CollectRunStatus.CANCELED, found, inserted, updated, skips, finishedAt);
    }

    private void finish(
            CollectRunStatus status,
            int found,
            int inserted,
            int updated,
            List<CollectRunSkip> skips,
            Instant finishedAt) {
        this.status = status;
        this.found = found;
        this.inserted = inserted;
        this.updated = updated;
        this.skipped = skips.size();
        this.skips.addAll(skips);
        this.finishedAt = finishedAt;
    }

    public void fail(String failureReason, Instant finishedAt) {
        this.status = CollectRunStatus.FAILED;
        this.failureReason = failureReason;
        this.finishedAt = finishedAt;
    }
}
