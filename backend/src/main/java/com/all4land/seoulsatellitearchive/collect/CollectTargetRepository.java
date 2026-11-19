package com.all4land.seoulsatellitearchive.collect;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CollectTargetRepository extends JpaRepository<CollectTarget, Long> {

    @Query("select t.id from CollectTarget t where t.enabled order by t.id")
    List<Long> findEnabledIds();

    @Query("select count(t) > 0 from CollectTarget t where t.collection.id = :collectionId")
    boolean existsForCollection(Long collectionId);

    // 서버가 여러 대여도 한 대만 이 대상의 정기 실행을 판단·등록한다.
    // 네이티브 쿼리로는 @Formula 칸을 채울 수 없어서 잠금만 하고 엔티티는 따로 읽는다
    @Query(value = """
            SELECT target_id FROM collect_target
            WHERE target_id = :targetId AND enabled
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Long> lockEnabled(Long targetId);
}
