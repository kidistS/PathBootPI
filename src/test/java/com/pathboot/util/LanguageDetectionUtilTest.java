package com.pathboot.util;

import com.pathboot.enums.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LanguageDetectionUtil – Unit Tests")
class LanguageDetectionUtilTest {

    private LanguageDetectionUtil languageDetectionUtil;

    @BeforeEach
    void setUp() {
        languageDetectionUtil = new LanguageDetectionUtil();
    }

    // ── English detection ─────────────────────────────────────────────────────

    @Test
    @DisplayName("detectLanguage – plain English text should return ENGLISH")
    void detectLanguage_englishText_shouldReturnEnglish() {
        String englishText = "What is the income tax rate in Norway?";
        assertThat(languageDetectionUtil.detectLanguage(englishText)).isEqualTo(Language.ENGLISH);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "How do I apply for unemployment benefits?",
        "What documents do I need for a work permit?",
        "When is the tax return deadline?"
    })
    @DisplayName("detectLanguage – various English sentences should return ENGLISH")
    void detectLanguage_variousEnglishSentences_shouldReturnEnglish(String text) {
        assertThat(languageDetectionUtil.detectLanguage(text)).isEqualTo(Language.ENGLISH);
    }

    // ── Norwegian detection ───────────────────────────────────────────────────

    @Test
    @DisplayName("detectLanguage – Norwegian text with ø should return NORWEGIAN")
    void detectLanguage_norwegianTextWithO_shouldReturnNorwegian() {
        String norwegianText = "Hvordan søker jeg om dagpenger?";
        assertThat(languageDetectionUtil.detectLanguage(norwegianText)).isEqualTo(Language.NORWEGIAN);
    }

    @Test
    @DisplayName("detectLanguage – Norwegian text with å should return NORWEGIAN")
    void detectLanguage_norwegianTextWithA_shouldReturnNorwegian() {
        String norwegianText = "Når må jeg levere skattemeldingen?";
        assertThat(languageDetectionUtil.detectLanguage(norwegianText)).isEqualTo(Language.NORWEGIAN);
    }

    @Test
    @DisplayName("detectLanguage – Norwegian text with æ should return NORWEGIAN")
    void detectLanguage_norwegianTextWithAe_shouldReturnNorwegian() {
        String norwegianText = "Hva er æresbetegnelsen for NAV?";
        assertThat(languageDetectionUtil.detectLanguage(norwegianText)).isEqualTo(Language.NORWEGIAN);
    }

    // ── Amharic detection ─────────────────────────────────────────────────────

    @Test
    @DisplayName("detectLanguage – Amharic text should return AMHARIC")
    void detectLanguage_amharicText_shouldReturnAmharic() {
        // "What is the tax rate?" in Amharic
        String amharicText = "የቀረጥ ደረጃ ምን ያህል ነው?";
        assertThat(languageDetectionUtil.detectLanguage(amharicText)).isEqualTo(Language.AMHARIC);
    }

    @Test
    @DisplayName("detectLanguage – Amharic mixed with numbers should return AMHARIC")
    void detectLanguage_amharicMixedWithNumbers_shouldReturnAmharic() {
        String amharicText = "ወደ ኖርዌይ 2024 ዓ.ም ኢሚግሬሽን";
        assertThat(languageDetectionUtil.detectLanguage(amharicText)).isEqualTo(Language.AMHARIC);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("detectLanguage – null input should return ENGLISH (default)")
    void detectLanguage_nullInput_shouldReturnEnglish() {
        assertThat(languageDetectionUtil.detectLanguage(null)).isEqualTo(Language.ENGLISH);
    }

    @Test
    @DisplayName("detectLanguage – blank input should return ENGLISH (default)")
    void detectLanguage_blankInput_shouldReturnEnglish() {
        assertThat(languageDetectionUtil.detectLanguage("   ")).isEqualTo(Language.ENGLISH);
    }

    @Test
    @DisplayName("detectLanguage – numeric-only input should default to ENGLISH")
    void detectLanguage_numericInput_shouldDefaultToEnglish() {
        assertThat(languageDetectionUtil.detectLanguage("12345")).isEqualTo(Language.ENGLISH);
    }
}

