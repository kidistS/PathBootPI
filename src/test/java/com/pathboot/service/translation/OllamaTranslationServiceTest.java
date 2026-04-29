package com.pathboot.service.translation;

import com.pathboot.exception.TranslationException;
import com.pathboot.util.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaTranslationService – Unit Tests")
class OllamaTranslationServiceTest {

    /** Deep stubs allow fluent ChatClient chain mocking without manual step-by-step setups. */
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock private PromptBuilder promptBuilder;

    private OllamaTranslationService service;

    private static final String NORWEGIAN_INPUT  = "Hva er skatten i Norge?";
    private static final String ENGLISH_INPUT    = "What is the tax rate in Norway?";
    private static final String TRANSLATED_EN    = "What is the tax rate in Norway?";
    private static final String TRANSLATED_NO    = "Hva er skattesatsen i Norge?";
    private static final String NO_EN_PROMPT     = "Translate Norwegian to English: " + NORWEGIAN_INPUT;
    private static final String EN_NO_PROMPT     = "Translate English to Norwegian: " + ENGLISH_INPUT;

    @BeforeEach
    void setUp() {
        service = new OllamaTranslationService(chatClient, promptBuilder);
    }

    // ── canHandleLanguagePair ─────────────────────────────────────────────────

    @Nested
    @DisplayName("canHandleLanguagePair")
    class CanHandleLanguagePair {

        @ParameterizedTest(name = "[{index}] {0} → {1} should return true")
        @CsvSource({
            "NORWEGIAN, ENGLISH",
            "ENGLISH,   NORWEGIAN",
            "norwegian, english",    // lowercase
            "English,   Norwegian"   // mixed case
        })
        @DisplayName("Norwegian ↔ English pairs should return true")
        void norwegianEnglishPairs_shouldReturnTrue(String src, String tgt) {
            assertThat(service.canHandleLanguagePair(src.trim(), tgt.trim())).isTrue();
        }

        @ParameterizedTest(name = "[{index}] {0} → {1} should return false")
        @CsvSource({
            "AMHARIC, ENGLISH",
            "ENGLISH, AMHARIC",
            "NORWEGIAN, AMHARIC",
            "AMHARIC,  NORWEGIAN",
            "ENGLISH,  ENGLISH",
            "NORWEGIAN, NORWEGIAN"
        })
        @DisplayName("Non-Norwegian/English pairs should return false")
        void nonNorwegianEnglishPairs_shouldReturnFalse(String src, String tgt) {
            assertThat(service.canHandleLanguagePair(src.trim(), tgt.trim())).isFalse();
        }
    }

    // ── translate – happy path ────────────────────────────────────────────────

    @Nested
    @DisplayName("translate – happy path")
    class TranslateHappyPath {

        @Test
        @DisplayName("NORWEGIAN → ENGLISH: builds correct prompt and returns LLM response")
        void norwegianToEnglish_buildsPromptAndCallsLlm() {
            when(promptBuilder.buildNorwegianToEnglishPrompt(NORWEGIAN_INPUT)).thenReturn(NO_EN_PROMPT);
            when(chatClient.prompt().user(NO_EN_PROMPT).call().content()).thenReturn(TRANSLATED_EN);

            String result = service.translate(NORWEGIAN_INPUT, "NORWEGIAN", "ENGLISH");

            assertThat(result).isEqualTo(TRANSLATED_EN);
            verify(promptBuilder, times(1)).buildNorwegianToEnglishPrompt(NORWEGIAN_INPUT);
        }

        @Test
        @DisplayName("ENGLISH → NORWEGIAN: builds correct prompt and returns LLM response")
        void englishToNorwegian_buildsPromptAndCallsLlm() {
            when(promptBuilder.buildEnglishToNorwegianPrompt(ENGLISH_INPUT)).thenReturn(EN_NO_PROMPT);
            when(chatClient.prompt().user(EN_NO_PROMPT).call().content()).thenReturn(TRANSLATED_NO);

            String result = service.translate(ENGLISH_INPUT, "ENGLISH", "NORWEGIAN");

            assertThat(result).isEqualTo(TRANSLATED_NO);
            verify(promptBuilder, times(1)).buildEnglishToNorwegianPrompt(ENGLISH_INPUT);
        }

        @Test
        @DisplayName("translation result is trimmed before returning")
        void translationResult_isTrimmed() {
            when(promptBuilder.buildNorwegianToEnglishPrompt(NORWEGIAN_INPUT)).thenReturn(NO_EN_PROMPT);
            when(chatClient.prompt().user(NO_EN_PROMPT).call().content())
                    .thenReturn("  " + TRANSLATED_EN + "\n  ");

            String result = service.translate(NORWEGIAN_INPUT, "NORWEGIAN", "ENGLISH");

            assertThat(result).isEqualTo(TRANSLATED_EN);
        }

        @Test
        @DisplayName("case-insensitive language codes are accepted (lowercase)")
        void lowercaseLanguageCodes_areAccepted() {
            when(promptBuilder.buildNorwegianToEnglishPrompt(NORWEGIAN_INPUT)).thenReturn(NO_EN_PROMPT);
            when(chatClient.prompt().user(NO_EN_PROMPT).call().content()).thenReturn(TRANSLATED_EN);

            String result = service.translate(NORWEGIAN_INPUT, "norwegian", "english");

            assertThat(result).isEqualTo(TRANSLATED_EN);
        }
    }

    // ── translate – error / edge cases ───────────────────────────────────────

    @Nested
    @DisplayName("translate – error cases")
    class TranslateErrorCases {

        @Test
        @DisplayName("LLM returns null → throws TranslationException")
        void llmReturnsNull_throwsTranslationException() {
            when(promptBuilder.buildNorwegianToEnglishPrompt(anyString())).thenReturn(NO_EN_PROMPT);
            when(chatClient.prompt().user(anyString()).call().content()).thenReturn(null);

            assertThatThrownBy(() -> service.translate(NORWEGIAN_INPUT, "NORWEGIAN", "ENGLISH"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("LLM returns blank string → throws TranslationException")
        void llmReturnsBlankString_throwsTranslationException() {
            when(promptBuilder.buildNorwegianToEnglishPrompt(anyString())).thenReturn(NO_EN_PROMPT);
            when(chatClient.prompt().user(anyString()).call().content()).thenReturn("   ");

            assertThatThrownBy(() -> service.translate(NORWEGIAN_INPUT, "NORWEGIAN", "ENGLISH"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("LLM throws runtime exception → wrapped in TranslationException")
        void llmThrowsRuntimeException_wrappedInTranslationException() {
            when(promptBuilder.buildNorwegianToEnglishPrompt(anyString())).thenReturn(NO_EN_PROMPT);
            when(chatClient.prompt().user(anyString()).call().content())
                    .thenThrow(new RuntimeException("Ollama unavailable"));

            assertThatThrownBy(() -> service.translate(NORWEGIAN_INPUT, "NORWEGIAN", "ENGLISH"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("Ollama translation service error");
        }

        @Test
        @DisplayName("TranslationException is rethrown directly — not double-wrapped")
        void translationException_isRethrown_notDoubleWrapped() {
            when(promptBuilder.buildNorwegianToEnglishPrompt(anyString())).thenReturn(NO_EN_PROMPT);
            TranslationException original = new TranslationException("original problem");
            when(chatClient.prompt().user(anyString()).call().content()).thenThrow(original);

            assertThatThrownBy(() -> service.translate(NORWEGIAN_INPUT, "NORWEGIAN", "ENGLISH"))
                    .isSameAs(original);
        }

        @Test
        @DisplayName("unsupported language pair (e.g., AMHARIC → ENGLISH) → throws TranslationException")
        void unsupportedLanguagePair_throwsTranslationException() {
            assertThatThrownBy(() -> service.translate("text", "AMHARIC", "ENGLISH"))
                    .isInstanceOf(TranslationException.class)
                    .hasMessageContaining("cannot handle pair");
        }

        @Test
        @DisplayName("unsupported language pair (NORWEGIAN → AMHARIC) → throws TranslationException")
        void norwegianToAmharic_throwsTranslationException() {
            assertThatThrownBy(() -> service.translate("text", "NORWEGIAN", "AMHARIC"))
                    .isInstanceOf(TranslationException.class);
        }
    }
}

