package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.global.paging.SliceResponse;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonSuccessCode;
import com.all4land.seoulsatellitearchive.image.dto.ImageDetailResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageFilterOptionsResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageResponse;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import com.all4land.seoulsatellitearchive.image.swagger.ImageApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

    @Override
    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody ImageWriteRequest request) {
        imageService.create(request);
        return ApiResponse.success(CommonSuccessCode.CREATED);
    }

    @Override
    @GetMapping("/{imageId}")
    public ApiResponse<ImageDetailResponse> detail(@PathVariable Long imageId) {
        return ApiResponse.success(CommonSuccessCode.OK, imageService.getDetail(imageId));
    }

    @Override
    @PutMapping("/{imageId}")
    public ApiResponse<Void> update(@PathVariable Long imageId, @Valid @RequestBody ImageWriteRequest request) {
        imageService.update(imageId, request);
        return ApiResponse.success(CommonSuccessCode.OK);
    }

    @Override
    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> delete(@PathVariable Long imageId) {
        imageService.delete(imageId);
        return ApiResponse.success(CommonSuccessCode.OK);
    }
}
