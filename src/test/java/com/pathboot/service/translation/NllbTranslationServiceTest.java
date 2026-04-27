package com.pathboot.service.translation;

import com.pathboot.exception.TranslationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NllbTranslationService – Unit Tests")
class NllbTranslationServiceTest {

    @Mock private RestTemplate nllbRestTemplate;

    private NllbTranslationService service;

    private static final String BASE_URL      = "http://localhost:5000";
    private static final String TRANSLATE_URL = BASE_URL + "/translate";
    private static final String WARMUP_URL    = BASE_URL + "/warmup";
    private static final String SAMPLE_TEXT   = "What is the tax rate?";
    private static final String TRANSLATED    = "የቀረጥ ደረጃ ምን ያህል ነው?";

    @BeforeEach
    void setUp() {
        service = new NllbTranslationService(nllbRestTemplate, TRANSLATE_URL, BASE_URL);
    }

    // ── canHandleLanguagePair ─────────────────────────────────────────────────

    @Nested
    @DisplayName("canHandleLanguagePair")
    class CanHandleLanguagePair {

        @ParameterizedTest(name = "[{index}] {0} → {1} should return true")
        @CsvSource({
            "AMHARIC, ENGLISH",
            "ENGLISH, AMHARIC",
            "amharic, english",   // lowercase
            "English, Amharic"    // mixed case
        })
        @DisplayName("Amharic ↔ English pairs should return true")
        void amharicEnglishPairs_shouldReturnTrue(String src, String tgt) {
            assertThat(service.canHandleLanguagePair(src, tgt)).isTrue();
        }

        @ParameterizedTest(name = "[{index}] {0} → {1} should return false")
        @CsvSource({
            "NORWEGIAN, ENGLISH",
            "ENGLISH,   NORWEGIAN",
            "NORWEGIAN, AMHARIC",
            "AMHARIC,   NORWEGIAN",
            "ENGLISH,   ENGLISH",
            "AMHARIC,   AMHARIC"
        })
        @DisplayName("Non-Amharic/English pairs should return false")
        void nonAmharicEnglishPairs_shouldReturnFalse(String src, String tgt) {
            assertThat(service.canHandleLanguagePair(src.trim(), tgt.trim())).isFalse();
        }
    }

    // ── translate – happy path ────────────────────────────────────────────────

    @Nested
    @DisplayName("translate – happy path")
    class TranslateHappyPath {

        @Test
        @DisplayName("ENGLISH → AMHARIC: successful translation returns translated text")
        @SuppressWarnings("unchecked")
        void englishToAmharic_successfulTranslation_shouldReturnTranslatedText() {
            mockSuccessfulResponse(TRANSLATED);

            String result = service.translate(SAMPLE_TEXT, "ENGLISH", "AMHARIC");

            assertThat(result).isEqualTo(TRANSLATED);
        }

        @Test
        @DisplayName("AMHARIC → ENGLISH: successful translation returns translated text")
        @SuppressWarnings("unchecked")
        void amharicToEnglish_successfulTranslation_shouldReturnTranslatedText() {
            mockSuccessfulResponse(SAMPLE_TEXT);

            String result = service.translate(TRANSLATED, "AMHARIC", "ENGLISH");

            assertThat(result).isEqualTo(SAMPLE_TEXT);
        }

        @Test
        @DisplayName("translation result is trimmed before returning")
        @SuppressWarnings("unchecked")
        void translationResult_shouldBeTrimmed() {
            mockSuccessfulResponse("  " + TRANSLATED + "\n");

            String result = service.translate(SAMPLE_TEXT, "ENGLISH", "AMHARIC");

            assertThat(result).isEqualTo(TRANSLATED);
        }
    }

    // ── translate – error / edge cases ───────────────────────────────────────

    @Nested
    @DisplayName("translate – error cases")
    class TranslateErrorCases {

        @Test
        @DisplayName("NLLB server returns null body → throws TranslationException")
        @SuppressWarnings("unchecked")
        void nullResponseBody_shouldThrowTranslationException() {
            when(nllbRestTemplate.exchange(eq(TRANSLATE_URL), eq(HttpMethod.POST), any(), any(Class.class)))
                    .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

            assertThatThrownBy(() -> service.translate(SAMPLE_TEXT, "ENGLISH", "AMHARIC"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("empty translation response");
        }

        @Test
        @DisplayName("NLLB server returns blank translated_text → throws TranslationException")
        @SuppressWarnings("unchecked")
        void blankTranslatedText_shouldThrowTranslationException() {
            mockSuccessfulResponse("   ");

            assertThatThrownBy(() -> service.translate(SAMPLE_TEXT, "ENGLISH", "AMHARIC"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("empty translation response");
        }

        @Test
        @DisplayName("ResourceAccessException on first call → retries once → succeeds")
        @SuppressWarnings("unchecked")
        void resourceAccessException_shouldRetryOnce_thenSucceed() {
            when(nllbRestTemplate.exchange(eq(TRANSLATE_URL), eq(HttpMethod.POST), any(), any(Class.class)))
                    .thenThrow(new ResourceAccessException("Connection timed out"))
                    .thenReturn(buildResponseEntity(TRANSLATED));

            String result = service.translate(SAMPLE_TEXT, "ENGLISH", "AMHARIC");

            assertThat(result).isEqualTo(TRANSLATED);
            // Called twice: first attempt (throws) + one retry (succeeds)
            verify(nllbRestTemplate, times(2))
                    .exchange(eq(TRANSLATE_URL), eq(HttpMethod.POST), any(), any(Class.class));
        }

        @Test
        @DisplayName("ResourceAccessException on both attempts → throws TranslationException with timeout message")
        @SuppressWarnings("unchecked")
        void resourceAccessException_bothAttemptsFail_shouldThrowTranslationException() {
            when(nllbRestTemplate.exchange(eq(TRANSLATE_URL), eq(HttpMethod.POST), any(), any(Class.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> service.translate(SAMPLE_TEXT, "ENGLISH", "AMHARIC"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("timed out");
        }

        @Test
        @DisplayName("RestClientException (non-I/O) → throws TranslationException immediately (no retry)")
        @SuppressWarnings("unchecked")
        void restClientException_shouldThrowTranslationExceptionImmediately() {
            when(nllbRestTemplate.exchange(eq(TRANSLATE_URL), eq(HttpMethod.POST), any(), any(Class.class)))
                    .thenThrow(new RestClientException("500 Internal Server Error"));

            assertThatThrownBy(() -> service.translate(SAMPLE_TEXT, "ENGLISH", "AMHARIC"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("Could not reach");

            // No retry for generic RestClientException
            verify(nllbRestTemplate, times(1))
                    .exchange(eq(TRANSLATE_URL), eq(HttpMethod.POST), any(), any(Class.class));
        }

        @Test
        @DisplayName("unsupported language (e.g., NORWEGIAN) → throws TranslationException")
        void unsupportedLanguage_shouldThrowTranslationException() {
            assertThatThrownBy(() -> service.translate(SAMPLE_TEXT, "NORWEGIAN", "ENGLISH"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("Unsupported NLLB language");
        }
    }

    // ── warmUpNllbServer ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("warmUpNllbServer")
    class WarmUp {

        @Test
        @DisplayName("warm-up succeeds → no exception thrown")
        @SuppressWarnings("unchecked")
        void warmUp_serverUp_shouldNotThrow() {
            when(nllbRestTemplate.exchange(eq(WARMUP_URL), eq(HttpMethod.POST), any(), any(Class.class)))
                    .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

            // Should complete without throwing
            service.warmUpNllbServer();

            verify(nllbRestTemplate, times(1))
                    .exchange(eq(WARMUP_URL), eq(HttpMethod.POST), any(), any(Class.class));
        }

        @Test
        @DisplayName("warm-up fails (server down) → warning logged, no exception thrown")
        @SuppressWarnings("unchecked")
        void warmUp_serverDown_shouldNotThrow() {
            when(nllbRestTemplate.exchange(eq(WARMUP_URL), eq(HttpMethod.POST), any(), any(Class.class)))
                    .thenThrow(new ResourceAccessException("Connection refused: localhost/5000"));

            // Must NOT throw — warm-up failure is non-fatal
            service.warmUpNllbServer();
        }

        @Test
        @DisplayName("warm-up – RuntimeException from server → swallowed, no propagation")
        @SuppressWarnings("unchecked")
        void warmUp_runtimeException_shouldBeSwallowed() {
            when(nllbRestTemplate.exchange(eq(WARMUP_URL), eq(HttpMethod.POST), any(), any(Class.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // Must NOT propagate
            service.warmUpNllbServer();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void mockSuccessfulResponse(String translatedText) {
        when(nllbRestTemplate.exchange(eq(TRANSLATE_URL), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(buildResponseEntity(translatedText));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity buildResponseEntity(String translatedText) {
        // Use reflection-free approach via a spy on the inner DTO
        NllbTranslationService.NllbTranslationResponse body =
                new NllbTranslationService.NllbTranslationResponse(translatedText);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}

