package com.pathboot.util;

import com.pathboot.enums.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptBuilder – Unit Tests")
class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    private static final String DOMAIN_NAME     = "Norwegian Tax (Skatt)";
    private static final String CONTEXT         = "Income tax rate is 22%.";
    private static final String NORWEGIAN_TEXT  = "Hva er skattesatsen?";
    private static final String ENGLISH_TEXT    = "What is the tax rate?";

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    // ── buildDomainSystemPrompt ───────────────────────────────────────────────

    @Nested
    @DisplayName("buildDomainSystemPrompt")
    class BuildDomainSystemPrompt {

        @Test
        @DisplayName("result contains the domain display name")
        void result_containsDomainDisplayName() {
            String prompt = promptBuilder.buildDomainSystemPrompt(DOMAIN_NAME, CONTEXT);
            assertThat(prompt).contains(DOMAIN_NAME);
        }

        @Test
        @DisplayName("result contains the grounding context")
        void result_containsGroundingContext() {
            String prompt = promptBuilder.buildDomainSystemPrompt(DOMAIN_NAME, CONTEXT);
            assertThat(prompt).contains(CONTEXT);
        }

        @Test
        @DisplayName("result is non-blank for any non-null inputs")
        void result_isNonBlank() {
            String prompt = promptBuilder.buildDomainSystemPrompt("Any Domain", "Some context");
            assertThat(prompt).isNotBlank();
        }

        @Test
        @DisplayName("does NOT contain language-specific instruction (standard prompt)")
        void result_doesNotContainLanguageInstruction() {
            String prompt = promptBuilder.buildDomainSystemPrompt(DOMAIN_NAME, CONTEXT);
            assertThat(prompt).doesNotContainIgnoringCase("respond only in");
        }

        @Test
        @DisplayName("different domains and contexts produce different prompts")
        void differentInputs_produceDifferentPrompts() {
            String taxPrompt = promptBuilder.buildDomainSystemPrompt("Tax", "tax context");
            String navPrompt = promptBuilder.buildDomainSystemPrompt("NAV", "nav context");
            assertThat(taxPrompt).isNotEqualTo(navPrompt);
        }

        @Test
        @DisplayName("empty context is included in the prompt without error")
        void emptyContext_includedWithoutError() {
            String prompt = promptBuilder.buildDomainSystemPrompt(DOMAIN_NAME, "");
            assertThat(prompt).contains(DOMAIN_NAME);
            assertThat(prompt).isNotBlank();
        }
    }

    // ── buildLanguageAwareDomainSystemPrompt ──────────────────────────────────

    @Nested
    @DisplayName("buildLanguageAwareDomainSystemPrompt")
    class BuildLanguageAwareDomainSystemPrompt {

        @Test
        @DisplayName("result contains the domain display name")
        void result_containsDomainDisplayName() {
            String prompt = promptBuilder.buildLanguageAwareDomainSystemPrompt(
                    DOMAIN_NAME, Language.NORWEGIAN, CONTEXT);
            assertThat(prompt).contains(DOMAIN_NAME);
        }

        @Test
        @DisplayName("result contains the grounding context")
        void result_containsGroundingContext() {
            String prompt = promptBuilder.buildLanguageAwareDomainSystemPrompt(
                    DOMAIN_NAME, Language.NORWEGIAN, CONTEXT);
            assertThat(prompt).contains(CONTEXT);
        }

        @Test
        @DisplayName("result contains Norwegian display name for NORWEGIAN language")
        void norwegianLanguage_containsNorwegianDisplayName() {
            String prompt = promptBuilder.buildLanguageAwareDomainSystemPrompt(
                    DOMAIN_NAME, Language.NORWEGIAN, CONTEXT);
            assertThat(prompt).containsIgnoringCase("norwegian");
        }

        @Test
        @DisplayName("result instructs LLM to respond in the target language")
        void result_containsLanguageResponseInstruction() {
            String prompt = promptBuilder.buildLanguageAwareDomainSystemPrompt(
                    DOMAIN_NAME, Language.NORWEGIAN, CONTEXT);
            // Template: "respond ONLY in %s"
            assertThat(prompt).containsIgnoringCase("respond");
        }

        @Test
        @DisplayName("ENGLISH language produces a valid prompt containing 'English'")
        void englishLanguage_containsEnglishDisplayName() {
            String prompt = promptBuilder.buildLanguageAwareDomainSystemPrompt(
                    DOMAIN_NAME, Language.ENGLISH, CONTEXT);
            assertThat(prompt).containsIgnoringCase("english");
        }

        @Test
        @DisplayName("language-aware prompt differs from standard prompt for same domain")
        void languageAwarePrompt_differsfromStandardPrompt() {
            String standard = promptBuilder.buildDomainSystemPrompt(DOMAIN_NAME, CONTEXT);
            String langAware = promptBuilder.buildLanguageAwareDomainSystemPrompt(
                    DOMAIN_NAME, Language.NORWEGIAN, CONTEXT);
            assertThat(standard).isNotEqualTo(langAware);
        }
    }

    // ── buildNorwegianToEnglishPrompt ─────────────────────────────────────────

    @Nested
    @DisplayName("buildNorwegianToEnglishPrompt")
    class BuildNorwegianToEnglishPrompt {

        @Test
        @DisplayName("result contains the source Norwegian text")
        void result_containsNorwegianText() {
            String prompt = promptBuilder.buildNorwegianToEnglishPrompt(NORWEGIAN_TEXT);
            assertThat(prompt).contains(NORWEGIAN_TEXT);
        }

        @Test
        @DisplayName("result is non-blank")
        void result_isNonBlank() {
            assertThat(promptBuilder.buildNorwegianToEnglishPrompt(NORWEGIAN_TEXT)).isNotBlank();
        }

        @Test
        @DisplayName("result mentions 'Norwegian' as the source language")
        void result_mentionsNorwegian() {
            String prompt = promptBuilder.buildNorwegianToEnglishPrompt(NORWEGIAN_TEXT);
            assertThat(prompt).containsIgnoringCase("norwegian");
        }

        @Test
        @DisplayName("result mentions 'English' as the target language")
        void result_mentionsEnglish() {
            String prompt = promptBuilder.buildNorwegianToEnglishPrompt(NORWEGIAN_TEXT);
            assertThat(prompt).containsIgnoringCase("english");
        }
    }

    // ── buildEnglishToNorwegianPrompt ─────────────────────────────────────────

    @Nested
    @DisplayName("buildEnglishToNorwegianPrompt")
    class BuildEnglishToNorwegianPrompt {

        @Test
        @DisplayName("result contains the source English text")
        void result_containsEnglishText() {
            String prompt = promptBuilder.buildEnglishToNorwegianPrompt(ENGLISH_TEXT);
            assertThat(prompt).contains(ENGLISH_TEXT);
        }

        @Test
        @DisplayName("result is non-blank")
        void result_isNonBlank() {
            assertThat(promptBuilder.buildEnglishToNorwegianPrompt(ENGLISH_TEXT)).isNotBlank();
        }

        @Test
        @DisplayName("result mentions 'Norwegian' as the target language")
        void result_mentionsNorwegian() {
            String prompt = promptBuilder.buildEnglishToNorwegianPrompt(ENGLISH_TEXT);
            assertThat(prompt).containsIgnoringCase("norwegian");
        }

        @Test
        @DisplayName("result mentions 'English' as the source language")
        void result_mentionsEnglish() {
            String prompt = promptBuilder.buildEnglishToNorwegianPrompt(ENGLISH_TEXT);
            assertThat(prompt).containsIgnoringCase("english");
        }

        @Test
        @DisplayName("Norwegian→English and English→Norwegian prompts are different")
        void toEnglishAndToNorwegianPrompts_areDifferent() {
            String toEnglish    = promptBuilder.buildNorwegianToEnglishPrompt("some text");
            String toNorwegian  = promptBuilder.buildEnglishToNorwegianPrompt("some text");
            assertThat(toEnglish).isNotEqualTo(toNorwegian);
        }
    }
}

