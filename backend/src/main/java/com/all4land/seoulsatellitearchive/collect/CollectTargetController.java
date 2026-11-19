package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.collect.dto.AoiPresetResponse;
import com.all4land.seoulsatellitearchive.collect.dto.CollectTargetRequest;
import com.all4land.seoulsatellitearchive.collect.dto.CollectTargetResponse;
import com.all4land.seoulsatellitearchive.collect.swagger.CollectTargetApi;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonSuccessCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collect-targets")
public class CollectTargetController implements CollectTargetApi {

    private final CollectTargetService collectTargetService;

    @Override
    @GetMapping
    public ApiResponse<List<CollectTargetResponse>> findAll() {
        return ApiResponse.success(CommonSuccessCode.OK, collectTargetService.findAll());
    }

    @Override
    @GetMapping("/default-aoi")
    public ApiResponse<AoiPresetResponse> getDefaultAoi() {
        return ApiResponse.success(CommonSuccessCode.OK, collectTargetService.getDefaultAoi());
    }

    @Override
    @PostMapping
    public ApiResponse<CollectTargetResponse> create(@Valid @RequestBody CollectTargetRequest request) {
        return ApiResponse.success(CommonSuccessCode.CREATED, collectTargetService.create(request));
    }

    @Override
    @PutMapping("/{targetId}")
    public ApiResponse<CollectTargetResponse> update(
            @PathVariable Long targetId, @Valid @RequestBody CollectTargetRequest request) {
        return ApiResponse.success(CommonSuccessCode.OK, collectTargetService.update(targetId, request));
    }

    @Override
    @DeleteMapping("/{targetId}")
    public ApiResponse<Void> delete(@PathVariable Long targetId) {
        collectTargetService.delete(targetId);
        return ApiResponse.success(CommonSuccessCode.OK);
    }
}
