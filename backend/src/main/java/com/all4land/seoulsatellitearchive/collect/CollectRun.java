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
}
