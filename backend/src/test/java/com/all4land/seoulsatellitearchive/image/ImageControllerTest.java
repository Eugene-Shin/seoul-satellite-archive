package com.all4land.seoulsatellitearchive.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// 로컬 전용 DX 시드(provider=dx)가 있어도 없어도 같은 결과가 나오는 조건으로만 검증한다
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void filterOptions() throws Exception {
        mockMvc.perform(get("/api/images/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.satellites", hasItems("LANDSAT-8", "SENTINEL-1A", "WV02")))
                .andExpect(jsonPath("$.data.instruments", hasItems("C-SAR", "OLI", "msi")))
                .andExpect(jsonPath("$.data.providers", hasSize(4)))
                .andExpect(jsonPath("$.data.sensorTypes", contains("OPTICAL", "SAR")));
    }

    @Test
    void instrumentsMatchAnySelected() throws Exception {
        mockMvc.perform(get("/api/images").param("instruments", "OLI", "C-SAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(4)));
    }

    @Test
    void keywordTreatsUnderscoreLiterally() throws Exception {
        mockMvc.perform(get("/api/images").param("keyword", "_").param("providers", "maxar", "nasa-cmr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].satellite", everyItem(is("SENTINEL-1A"))))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void acquiredDateRangeCoversWholeKstDay() throws Exception {
        mockMvc.perform(get("/api/images").param("acquiredFrom", "2026-01-08").param("acquiredTo", "2026-01-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void cloudMaxExcludesSar() throws Exception {
        mockMvc.perform(get("/api/images").param("cloudMax", "100").param("sensorTypes", "SAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    void bboxFiltersByFootprint() throws Exception {
        mockMvc.perform(get("/api/images").param("bbox", "128.9,35.0,129.2,35.3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].footprint.type").value("MultiPolygon"));
    }

    @Test
    void cloudSortPutsNullsLast() throws Exception {
        mockMvc.perform(get("/api/images")
                        .param("providers", "earth-search", "nasa-cmr")
                        .param("sorts", "CLOUD_COVER_DESC")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].cloudCover").value(100.00))
                .andExpect(jsonPath("$.data.content[-1].cloudCover", nullValue()));
    }

    @Test
    void cursorContinuesUntilNull() throws Exception {
        String first = search(null, "5", "providers", "earth-search");
        assertThat(ids(first)).hasSize(5);
        String second = search(nextCursor(first), "5", "providers", "earth-search");
        assertThat(ids(second)).hasSize(5);
        String third = search(nextCursor(second), "5", "providers", "earth-search");
        assertThat(ids(third)).hasSize(2);
        assertThat(nextCursor(third)).isNull();
    }

    // 작은 크기로 끝까지 이어 받은 결과가 한 번에 받은 결과와 순서까지 같아야 한다 (동점·NULL·방향 혼합 포함)
    @ParameterizedTest
    @ValueSource(
            strings = {
                "ACQUIRED_AT_DESC",
                "ACQUIRED_AT_ASC",
                "CLOUD_COVER_ASC",
                "CLOUD_COVER_ASC,ACQUIRED_AT_DESC",
                "CLOUD_COVER_DESC,ACQUIRED_AT_ASC"
            })
    void cursorWalkMatchesSingleRequest(String sorts) throws Exception {
        String all = search(null, "100", "sorts", sorts);
        assertThat(nextCursor(all)).isNull();

        List<Integer> walked = new ArrayList<>();
        String cursor = null;
        do {
            String page = search(cursor, "3", "sorts", sorts);
            walked.addAll(ids(page));
            cursor = nextCursor(page);
        } while (cursor != null);

        assertThat(walked).isEqualTo(ids(all));
    }

    @Test
    void invalidCursorReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/images").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_001"));

        String cursorByDate = nextCursor(search(null, "1", "sorts", "ACQUIRED_AT_DESC"));
        mockMvc.perform(get("/api/images").param("cursor", cursorByDate).param("sorts", "CLOUD_COVER_ASC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("커서가 올바르지 않습니다."));
    }

    private String search(String cursor, String size, String name, String value) throws Exception {
        MockHttpServletRequestBuilder request =
                get("/api/images").param("size", size).param(name, value.split(","));
        if (cursor != null) {
            request.param("cursor", cursor);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<Integer> ids(String body) {
        return JsonPath.read(body, "$.data.content[*].imageId");
    }

    private static String nextCursor(String body) {
        return JsonPath.read(body, "$.data.nextCursor");
    }

    @Test
    void invalidParamsReturnValidationError() throws Exception {
        mockMvc.perform(get("/api/images").param("bbox", "126.9,37.5,127.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"));
        mockMvc.perform(get("/api/images").param("sorts", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("sorts: 타입이 올바르지 않습니다."));
        mockMvc.perform(get("/api/images").param("sorts", "ACQUIRED_AT_ASC", "CLOUD_COVER_ASC", "ACQUIRED_AT_DESC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("sorts: 같은 정렬 기준은 한 번만 지정할 수 있습니다."));
    }
}
