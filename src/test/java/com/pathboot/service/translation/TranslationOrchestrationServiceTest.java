package com.pathboot.service.translation;

import com.pathboot.enums.Language;
import com.pathboot.exception.TranslationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranslationOrchestrationService – Unit Tests")
class TranslationOrchestrationServiceTest {

    @Mock private TranslationService nllbTranslationService;
    @Mock private TranslationService ollamaTranslationService;

    private TranslationOrchestrationService service;

    private static final String INPUT_TEXT     = "Sample text";
    private static final String TRANSLATED_EN  = "Translated to English";
    private static final String TRANSLATED_AM  = "ምሳሌ ጽሑፍ";
    private static final String TRANSLATED_NO  = "Oversatt til norsk";

    @BeforeEach
    void setUp() {
        service = new TranslationOrchestrationService(ollamaTranslationService, nllbTranslationService);
    }

    // ── translateToEnglish ────────────────────────────────────────────────────

    @Nested
    @DisplayName("translateToEnglish")
    class TranslateToEnglish {

        @Test
        @DisplayName("ENGLISH input → returns original text unchanged (no service call)")
        void englishInput_shouldReturnOriginalTextUnchanged() {
            String result = service.translateToEnglish(INPUT_TEXT, Language.ENGLISH);
            assertThat(result).isEqualTo(INPUT_TEXT);
            verifyNoInteractions(nllbTranslationService, ollamaTranslationService);
        }

        @Test
        @DisplayName("UNKNOWN input → returns original text unchanged (no service call)")
        void unknownInput_shouldReturnOriginalTextUnchanged() {
            String result = service.translateToEnglish(INPUT_TEXT, Language.UNKNOWN);
            assertThat(result).isEqualTo(INPUT_TEXT);
            verifyNoInteractions(nllbTranslationService, ollamaTranslationService);
        }

        @Test
        @DisplayName("AMHARIC input → routed to NLLB service")
        void amharicInput_shouldRouteToNllbService() {
            when(nllbTranslationService.canHandleLanguagePair("AMHARIC", "ENGLISH")).thenReturn(true);
            when(nllbTranslationService.translate(INPUT_TEXT, "AMHARIC", "ENGLISH")).thenReturn(TRANSLATED_EN);

            String result = service.translateToEnglish(INPUT_TEXT, Language.AMHARIC);

            assertThat(result).isEqualTo(TRANSLATED_EN);
            verify(nllbTranslationService, times(1)).translate(INPUT_TEXT, "AMHARIC", "ENGLISH");
            verifyNoInteractions(ollamaTranslationService);
        }

        @Test
        @DisplayName("NORWEGIAN input → routed to Ollama service")
        void norwegianInput_shouldRouteToOllamaService() {
            when(nllbTranslationService.canHandleLanguagePair("NORWEGIAN", "ENGLISH")).thenReturn(false);
            when(ollamaTranslationService.canHandleLanguagePair("NORWEGIAN", "ENGLISH")).thenReturn(true);
            when(ollamaTranslationService.translate(INPUT_TEXT, "NORWEGIAN", "ENGLISH")).thenReturn(TRANSLATED_EN);

            String result = service.translateToEnglish(INPUT_TEXT, Language.NORWEGIAN);

            assertThat(result).isEqualTo(TRANSLATED_EN);
            verify(ollamaTranslationService, times(1)).translate(INPUT_TEXT, "NORWEGIAN", "ENGLISH");
            verify(nllbTranslationService, never()).translate(any(), any(), any());
        }

        @Test
        @DisplayName("unsupported language pair → throws TranslationException")
        void unsupportedLanguagePair_shouldThrowTranslationException() {
            when(nllbTranslationService.canHandleLanguagePair(anyString(), anyString())).thenReturn(false);
            when(ollamaTranslationService.canHandleLanguagePair(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> service.translateToEnglish(INPUT_TEXT, Language.NORWEGIAN))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("No translation service available");
        }
    }

    // ── translateFromEnglish ──────────────────────────────────────────────────

    @Nested
    @DisplayName("translateFromEnglish")
    class TranslateFromEnglish {

        @Test
        @DisplayName("ENGLISH target → returns original text unchanged (no service call)")
        void englishTarget_shouldReturnOriginalTextUnchanged() {
            String result = service.translateFromEnglish(INPUT_TEXT, Language.ENGLISH);
            assertThat(result).isEqualTo(INPUT_TEXT);
            verifyNoInteractions(nllbTranslationService, ollamaTranslationService);
        }

        @Test
        @DisplayName("UNKNOWN target → returns original text unchanged (no service call)")
        void unknownTarget_shouldReturnOriginalTextUnchanged() {
            String result = service.translateFromEnglish(INPUT_TEXT, Language.UNKNOWN);
            assertThat(result).isEqualTo(INPUT_TEXT);
            verifyNoInteractions(nllbTranslationService, ollamaTranslationService);
        }

        @Test
        @DisplayName("AMHARIC target → routed to NLLB service")
        void amharicTarget_shouldRouteToNllbService() {
            when(nllbTranslationService.canHandleLanguagePair("ENGLISH", "AMHARIC")).thenReturn(true);
            when(nllbTranslationService.translate(INPUT_TEXT, "ENGLISH", "AMHARIC")).thenReturn(TRANSLATED_AM);

            String result = service.translateFromEnglish(INPUT_TEXT, Language.AMHARIC);

            assertThat(result).isEqualTo(TRANSLATED_AM);
            verify(nllbTranslationService, times(1)).translate(INPUT_TEXT, "ENGLISH", "AMHARIC");
            verifyNoInteractions(ollamaTranslationService);
        }

        @Test
        @DisplayName("NORWEGIAN target → routed to Ollama service")
        void norwegianTarget_shouldRouteToOllamaService() {
            when(nllbTranslationService.canHandleLanguagePair("ENGLISH", "NORWEGIAN")).thenReturn(false);
            when(ollamaTranslationService.canHandleLanguagePair("ENGLISH", "NORWEGIAN")).thenReturn(true);
            when(ollamaTranslationService.translate(INPUT_TEXT, "ENGLISH", "NORWEGIAN")).thenReturn(TRANSLATED_NO);

            String result = service.translateFromEnglish(INPUT_TEXT, Language.NORWEGIAN);

            assertThat(result).isEqualTo(TRANSLATED_NO);
            verify(ollamaTranslationService, times(1)).translate(INPUT_TEXT, "ENGLISH", "NORWEGIAN");
            verify(nllbTranslationService, never()).translate(any(), any(), any());
        }

        @Test
        @DisplayName("empty English text translated to Amharic → returns translated result")
        void emptyEnglishText_translatedToAmharic_shouldReturnTranslatedResult() {
            when(nllbTranslationService.canHandleLanguagePair("ENGLISH", "AMHARIC")).thenReturn(true);
            when(nllbTranslationService.translate("", "ENGLISH", "AMHARIC")).thenReturn("");

            String result = service.translateFromEnglish("", Language.AMHARIC);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("unsupported language pair → throws TranslationException")
        void unsupportedLanguagePair_shouldThrowTranslationException() {
            when(nllbTranslationService.canHandleLanguagePair(anyString(), anyString())).thenReturn(false);
            when(ollamaTranslationService.canHandleLanguagePair(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> service.translateFromEnglish(INPUT_TEXT, Language.NORWEGIAN))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("No translation service available");
        }

        @Test
        @DisplayName("NLLB preferred over Ollama when NLLB can handle the pair")
        void nllbPreferredOverOllama_whenNllbCanHandlePair() {
            when(nllbTranslationService.canHandleLanguagePair("ENGLISH", "AMHARIC")).thenReturn(true);
            when(nllbTranslationService.translate(INPUT_TEXT, "ENGLISH", "AMHARIC")).thenReturn(TRANSLATED_AM);

            service.translateFromEnglish(INPUT_TEXT, Language.AMHARIC);

            verify(nllbTranslationService).translate(INPUT_TEXT, "ENGLISH", "AMHARIC");
            // Ollama canHandleLanguagePair should NOT even be called when NLLB handles it
            verify(ollamaTranslationService, never()).canHandleLanguagePair(anyString(), anyString());
        }
    }
}

