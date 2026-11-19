package com.all4land.seoulsatellitearchive.collect;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CollectBandRuleRepository extends JpaRepository<CollectBandRule, Long> {

    @Query("""
            select r from CollectBandRule r
            where r.collectionId = :collectionId
            order by r.fileSuffix, r.bandIndex
            """)
    List<CollectBandRule> findByCollection(Long collectionId);
}
