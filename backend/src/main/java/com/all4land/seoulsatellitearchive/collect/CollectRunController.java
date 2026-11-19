package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.collect.dto.CollectRunRequest;
import com.all4land.seoulsatellitearchive.collect.dto.CollectRunResponse;
import com.all4land.seoulsatellitearchive.collect.dto.CollectRunSearchRequest;
import com.all4land.seoulsatellitearchive.collect.dto.CollectRunSkipResponse;
import com.all4land.seoulsatellitearchive.collect.swagger.CollectRunApi;
import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonSuccessCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CollectRunController implements CollectRunApi {

    private final CollectService collectService;

    // API로 등록하는 실행은 단건 실행이다. 워커가 가져가 실행하므로 접수만 하고 돌려준다
    @Override
    @PostMapping("/collect-targets/{targetId}/runs")
    public ApiResponse<CollectRunResponse> enqueue(
            @PathVariable Long targetId, @RequestBody(required = false) CollectRunRequest request) {
        CollectRunRequest window = request == null ? new CollectRunRequest(null, null) : request;
        Long runId = collectService.enqueue(
                targetId, CollectRunType.ONE_OFF, window.windowFromInstant(), window.windowToInstant());
        return ApiResponse.success(CommonSuccessCode.ACCEPTED, collectService.getRun(runId));
    }

    @Override
    @GetMapping("/collect-runs")
    public ApiResponse<SliceResponse<CollectRunResponse>> findRuns(
            @Valid @ParameterObject CollectRunSearchRequest request) {
        return ApiResponse.success(CommonSuccessCode.OK, collectService.findRuns(request));
    }

    @Override
    @GetMapping("/collect-runs/{runId}")
    public ApiResponse<CollectRunResponse> getRun(@PathVariable Long runId) {
        return ApiResponse.success(CommonSuccessCode.OK, collectService.getRun(runId));
    }

    @Override
    @GetMapping("/collect-runs/{runId}/skips")
    public ApiResponse<List<CollectRunSkipResponse>> getSkips(@PathVariable Long runId) {
        return ApiResponse.success(CommonSuccessCode.OK, collectService.getSkips(runId));
    }

    @Override
    @PostMapping("/collect-runs/{runId}/cancel")
    public ApiResponse<CollectRunResponse> cancel(@PathVariable Long runId) {
        collectService.requestCancel(runId);
        return ApiResponse.success(CommonSuccessCode.OK, collectService.getRun(runId));
    }
}
