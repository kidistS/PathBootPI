package com.pathboot.exception;

import com.pathboot.model.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for {@link GlobalExceptionHandler} using a tiny stub controller
 * that throws each exception type on demand.
 *
 * <p>Standalone MockMvc setup is used so no Spring Security or application
 * context is required — only the handler and the stub controller are loaded.</p>
 */
@DisplayName("GlobalExceptionHandler – Unit Tests")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /** Minimal REST controller that throws a specific exception on every endpoint. */
    @RestController
    static class StubController {
        @GetMapping("/test/domain-not-found")
        public ChatResponse domainNotFound() {
            throw new DomainNotFoundException("No agent for UNKNOWN");
        }
        @GetMapping("/test/translation-error")
        public ChatResponse translationError() {
            throw new TranslationException("NLLB server returned 500 — internal details");
        }
        @GetMapping("/test/llm-error")
        public ChatResponse llmError() {
            throw new LlmCommunicationException("Ollama connection refused");
        }
        @GetMapping("/test/generic-error")
        public ChatResponse genericError() {
            throw new RuntimeException("Unexpected NullPointerException");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── DomainNotFoundException ───────────────────────────────────────────────

    @Nested
    @DisplayName("DomainNotFoundException → 400 Bad Request")
    class DomainNotFound {

        @Test
        @DisplayName("returns HTTP 400")
        void returns400() throws Exception {
            mockMvc.perform(get("/test/domain-not-found"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("response is JSON with responseText field")
        void responseIsJsonWithResponseText() throws Exception {
            mockMvc.perform(get("/test/domain-not-found"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.responseText").exists());
        }

        @Test
        @DisplayName("responseText does NOT start with 'ERROR:'")
        void responseText_noErrorPrefix() throws Exception {
            mockMvc.perform(get("/test/domain-not-found"))
                    .andExpect(jsonPath("$.responseText").value(
                            org.hamcrest.Matchers.not(org.hamcrest.Matchers.startsWith("ERROR:"))));
        }
    }

    // ── TranslationException ─────────────────────────────────────────────────

    @Nested
    @DisplayName("TranslationException → 503 Service Unavailable")
    class TranslationError {

        @Test
        @DisplayName("returns HTTP 503")
        void returns503() throws Exception {
            mockMvc.perform(get("/test/translation-error"))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("responseText is generic — does NOT contain internal exception message")
        void responseText_noInternalDetails() throws Exception {
            mockMvc.perform(get("/test/translation-error"))
                    .andExpect(jsonPath("$.responseText").value(
                            org.hamcrest.Matchers.not(
                                    org.hamcrest.Matchers.containsString("NLLB server returned 500"))));
        }

        @Test
        @DisplayName("responseText mentions 'unavailable'")
        void responseText_mentionsUnavailable() throws Exception {
            mockMvc.perform(get("/test/translation-error"))
                    .andExpect(jsonPath("$.responseText").value(
                            org.hamcrest.Matchers.containsStringIgnoringCase("unavailable")));
        }
    }

    // ── LlmCommunicationException ─────────────────────────────────────────────

    @Nested
    @DisplayName("LlmCommunicationException → 503 Service Unavailable")
    class LlmError {

        @Test
        @DisplayName("returns HTTP 503")
        void returns503() throws Exception {
            mockMvc.perform(get("/test/llm-error"))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("responseText is a user-friendly message — no stack trace or internal detail")
        void responseText_isUserFriendly() throws Exception {
            mockMvc.perform(get("/test/llm-error"))
                    .andExpect(jsonPath("$.responseText").value(
                            org.hamcrest.Matchers.not(
                                    org.hamcrest.Matchers.containsString("connection refused"))));
        }
    }

    // ── Catch-all Exception ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Unexpected exception → 500 Internal Server Error")
    class GenericError {

        @Test
        @DisplayName("returns HTTP 500")
        void returns500() throws Exception {
            mockMvc.perform(get("/test/generic-error"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("responseText contains generic support message")
        void responseText_isGenericMessage() throws Exception {
            mockMvc.perform(get("/test/generic-error"))
                    .andExpect(jsonPath("$.responseText").value(
                            org.hamcrest.Matchers.containsStringIgnoringCase("unexpected")));
        }
    }

    // ── Common response shape ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Common response shape on all errors")
    class ResponseShape {

        @Test
        @DisplayName("every error response includes detectedLanguage = UNKNOWN")
        void everyError_hasDetectedLanguageUnknown() throws Exception {
            mockMvc.perform(get("/test/llm-error"))
                    .andExpect(jsonPath("$.detectedLanguage").value("UNKNOWN"));
        }

        @Test
        @DisplayName("every error response includes detectedDomain = UNKNOWN")
        void everyError_hasDetectedDomainUnknown() throws Exception {
            mockMvc.perform(get("/test/domain-not-found"))
                    .andExpect(jsonPath("$.detectedDomain").value("UNKNOWN"));
        }

        @Test
        @DisplayName("every error response includes a timestamp")
        void everyError_hasTimestamp() throws Exception {
            mockMvc.perform(get("/test/translation-error"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}

