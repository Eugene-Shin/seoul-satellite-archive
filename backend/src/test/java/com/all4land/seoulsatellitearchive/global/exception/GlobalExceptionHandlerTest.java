package com.all4land.seoulsatellitearchive.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import jakarta.servlet.RequestDispatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(ApiErrorController.class)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void businessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_409_001"))
                .andExpect(jsonPath("$.errorDetail[0]").value("이미 처리된 요청"));
    }

    @Test
    void validationFailure() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"))
                .andExpect(jsonPath("$.errorDetail[0]").value(org.hamcrest.Matchers.startsWith("name: ")));
    }

    @Test
    void bindingFailureHidesInternalTypeNames() throws Exception {
        mockMvc.perform(get("/test/bind").param("count", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"))
                .andExpect(jsonPath("$.errorDetail[0]").value("count: 타입이 올바르지 않습니다."));
    }

    @Test
    void unknownPath() throws Exception {
        mockMvc.perform(get("/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404_001"));
    }

    @Test
    void unexpectedExceptionHidesDetail() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_500_001"))
                .andExpect(jsonPath("$.errorDetail").doesNotExist());
    }

    @Test
    void errorKeepsJsonAndStatusEvenWhenClientAcceptsXml() throws Exception {
        mockMvc.perform(get("/test/business").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void errorDispatchReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404_001"));
    }

    @Test
    void onlyImageSourceUniqueViolationIsConflict() throws Exception {
        mockMvc.perform(get("/test/integrity")
                        .param("constraint", "uq_image_source")
                        .param("state", "23505"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMON_409_001"));
        mockMvc.perform(get("/test/integrity")
                        .param("constraint", "other_unique")
                        .param("state", "23505"))
                .andExpect(status().isInternalServerError());
        mockMvc.perform(get("/test/integrity")
                        .param("constraint", "uq_image_source")
                        .param("state", "23514"))
                .andExpect(status().isInternalServerError());
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/integrity")
        void integrity(
                @org.springframework.web.bind.annotation.RequestParam String constraint,
                @org.springframework.web.bind.annotation.RequestParam String state) {
            throw new DataIntegrityViolationException(
                    "database detail",
                    new org.hibernate.exception.ConstraintViolationException(
                            "constraint violation", new SQLException("database detail", state), "insert", constraint));
        }

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(CommonErrorCode.CONFLICT, "이미 처리된 요청");
        }

        @PostMapping("/test/valid")
        void valid(@Valid @RequestBody NameRequest request) {}

        @GetMapping("/test/bind")
        void bind(@Valid @ModelAttribute CountQuery query) {}

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new RuntimeException("DB password=secret");
        }
    }

    record NameRequest(@NotBlank String name) {}

    record CountQuery(Integer count) {}
}
