package com.all4land.seoulsatellitearchive.collect;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CollectSarRuleRepository extends JpaRepository<CollectSarRule, Long> {

    @Query("select r from CollectSarRule r where r.collectionId = :collectionId")
    List<CollectSarRule> findByCollection(Long collectionId);
}
