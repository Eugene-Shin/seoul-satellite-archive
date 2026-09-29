package com.all4land.seoulsatellitearchive;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SeoulSatelliteArchiveApplicationTests {

    @Test
    void contextLoads() {}
}
