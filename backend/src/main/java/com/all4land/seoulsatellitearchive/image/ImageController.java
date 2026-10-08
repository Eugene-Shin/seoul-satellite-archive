package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonSuccessCode;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import com.all4land.seoulsatellitearchive.image.swagger.ImageApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController implements ImageApi {

    private final ImageService imageService;

    @Override
    @GetMapping
    public ApiResponse<SliceResponse<ImageResponse>> search(@Valid @ParameterObject ImageSearchRequest request) {
        return ApiResponse.success(CommonSuccessCode.OK, imageService.search(request));
    }

    @Override
    @GetMapping("/filter-options")
    public ApiResponse<ImageFilterOptionsResponse> getFilterOptions() {
        return ApiResponse.success(CommonSuccessCode.OK, imageService.getFilterOptions());
    }
}
