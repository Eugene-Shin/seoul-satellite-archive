package com.all4land.seoulsatellitearchive.collect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record CollectTargetRequest(
        @NotNull @Schema(description = "수집할 컬렉션. collectable인 컬렉션만 고를 수 있고, 만든 뒤에는 바꿀 수 없다", example = "4")
        Long collectionId,

        @NotNull @Schema(
                type = "object",
                description = "수집 영역. 경위도 GeoJSON MultiPolygon. 넓히면 새로 들어온 지역은 lookback 기간만큼만 받는다",
                example =
                        "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[126.9,37.5],[127.1,37.5],[127.1,37.6],[126.9,37.5]]]]}")
        JsonNode aoi,

        @NotBlank @Schema(description = "Spring cron(초 분 시 일 월 요일). 한국 시간으로 해석한다", example = "0 0 3 * * *")
        String scheduleCron,

        @NotNull @Min(value = 1, message = "1~31일이어야 합니다.") @Max(value = 31, message = "1~31일이어야 합니다.") @Schema(description = "늦게 공개·갱신되는 영상을 다시 조회할 일수(1~31)", example = "7")
        Integer lookbackDays,

        @NotNull @Schema(description = "끄면 정기 수집을 하지 않는다. 단발 실행은 할 수 있다", example = "true")
        Boolean enabled) {}
