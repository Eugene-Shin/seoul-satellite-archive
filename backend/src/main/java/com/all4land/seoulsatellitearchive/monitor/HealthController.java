package com.all4land.seoulsatellitearchive.monitor;

import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonSuccessCode;
import com.all4land.seoulsatellitearchive.monitor.swagger.HealthApi;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements HealthApi {

    @Override
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(CommonSuccessCode.OK, Map.of("health", "UP"));
    }
}
