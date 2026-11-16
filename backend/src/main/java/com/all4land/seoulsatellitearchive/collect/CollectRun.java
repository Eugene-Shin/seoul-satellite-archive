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
    private CollectRunStatus status;

    private Instant windowFrom;

    private Instant windowTo;

    private int found;

    private int inserted;

    private int updated;

    private int skipped;

    private String failureReason;

    private Instant startedAt;

    private Instant finishedAt;

    @ElementCollection
    @CollectionTable(name = "collect_run_skip", joinColumns = @JoinColumn(name = "run_id"))
    private List<CollectRunSkip> skips = new ArrayList<>();

    public CollectRun(CollectTarget target, Instant windowFrom, Instant windowTo, Instant startedAt) {
        this.target = target;
        this.status = CollectRunStatus.RUNNING;
        this.windowFrom = windowFrom;
        this.windowTo = windowTo;
        this.startedAt = startedAt;
    }

    public void succeed(int found, int inserted, int updated, List<CollectRunSkip> skips, Instant finishedAt) {
        this.status = CollectRunStatus.SUCCEEDED;
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
