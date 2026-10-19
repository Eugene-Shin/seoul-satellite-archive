package com.all4land.seoulsatellitearchive.image;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SatelliteImageRepository
        extends JpaRepository<SatelliteImage, Long>, JpaSpecificationExecutor<SatelliteImage> {

    boolean existsByProvider_CodeAndSourceId(String providerCode, String sourceId);

    boolean existsByProvider_CodeAndSourceIdAndIdNot(String providerCode, String sourceId, Long id);

    @Query("select distinct i.satellite from SatelliteImage i order by i.satellite")
    List<String> findDistinctSatellites();

    @Query(value = "SELECT DISTINCT unnest(instruments) FROM satellite_image ORDER BY 1", nativeQuery = true)
    List<String> findDistinctInstruments();

    @Query("""
            select min(i.gsdMeters) as gsdMin, max(i.gsdMeters) as gsdMax,
                   min(i.acquiredAt) as acquiredMin, max(i.acquiredAt) as acquiredMax
            from SatelliteImage i
            """)
    Ranges findRanges();

    interface Ranges {
        BigDecimal getGsdMin();

        BigDecimal getGsdMax();

        Instant getAcquiredMin();

        Instant getAcquiredMax();
    }
}
