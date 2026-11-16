package com.all4land.seoulsatellitearchive.collect;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CollectRunRepository extends JpaRepository<CollectRun, Long> {

    @Query("""
            select r from CollectRun r
            where r.target.id = :targetId
              and r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.SUCCEEDED
            order by r.windowTo desc
            limit 1
            """)
    Optional<CollectRun> findLastSucceeded(Long targetId);
}
