package com.all4land.seoulsatellitearchive.collection;

import com.all4land.seoulsatellitearchive.collection.dto.CollectionResponse;
import com.all4land.seoulsatellitearchive.collection.swagger.CollectionApi;
import com.all4land.seoulsatellitearchive.global.response.ApiResponse;
import com.all4land.seoulsatellitearchive.global.response.CommonSuccessCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collections")
public class CollectionController implements CollectionApi {

    private final CollectionService collectionService;

    @Override
    @GetMapping
    public ApiResponse<List<CollectionResponse>> findAll() {
        return ApiResponse.success(CommonSuccessCode.OK, collectionService.findAll());
    }
}
