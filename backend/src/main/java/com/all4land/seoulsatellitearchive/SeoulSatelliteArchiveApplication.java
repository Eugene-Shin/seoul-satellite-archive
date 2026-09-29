package com.all4land.seoulsatellitearchive;

import com.all4land.seoulsatellitearchive.global.config.ClockConfig;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SeoulSatelliteArchiveApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ClockConfig.KST));
        SpringApplication.run(SeoulSatelliteArchiveApplication.class, args);
    }
}
