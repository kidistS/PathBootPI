package com.pathboot.util;

import com.pathboot.enums.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Utility for detecting the language of a given text string.
 *
 * <h3>Detection strategy</h3>
 * <ol>
 *   <li><b>Amharic</b> – presence of characters in the Ethiopic Unicode block (U+1200–U+137F).</li>
 *   <li><b>Norwegian</b> – presence of Norwegian-specific characters (æ, ø, å) <em>or</em>
 *       presence of uniquely Norwegian indicator words (e.g. "hva", "ikke", "dagpenger").</li>
 *   <li><b>English</b> – default when neither of the above heuristics trigger.</li>
 * </ol>
 */
@Component
public class LanguageDetectionUtil {

    private static final Logger logger = LogManager.getLogger(LanguageDetectionUtil.class);

    // ── Amharic ───────────────────────────────────────────────────────────────
    private static final int ETHIOPIC_BLOCK_START = 0x1200;
    private static final int ETHIOPIC_BLOCK_END   = 0x137F;

    // ── Norwegian special characters ──────────────────────────────────────────
    private static final String NORWEGIAN_SPECIAL_CHARS = "æøåÆØÅ";

    /**
     * Words that exist in Norwegian but NOT in English.
     * Checked as whole words (split on non-letter boundaries) to avoid false positives.
     */
    private static final Set<String> NORWEGIAN_INDICATOR_WORDS = Set.of(
        // Question words
        "hva", "hvem", "hvordan", "hvorfor",
        // Common Norwegian-only words
        "ikke", "også", "dette", "disse",
        "jeg", "deg", "seg",
        "eller", "fordi",
        // Country / language
        "norsk", "norge", "norske", "norges",
        // Tax domain (Norwegian)
        "skatt", "skatten", "skattemelding", "skattekort",
        "inntekt", "fradrag", "trinnskatt", "altinn",
        // NAV domain (Norwegian)
        "dagpenger", "sykepenger", "sykmelding", "sykmeldt",
        "arbeidsledig", "trygd", "ytelse", "arbeidsgiver",
        // Immigration domain (Norwegian)
        "innvandring", "oppholdstillatelse", "barnehage",
        "statsborgerskap", "familiegjenforening", "utlendingsdirektorat"
    );

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Detects the language of the provided {@code text}.
     *
     * @param text the input text to analyse
     * @return the detected {@link Language}; defaults to {@link Language#ENGLISH} when uncertain
     */
    public Language detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            logger.warn("detectLanguage called with null/blank text – defaulting to ENGLISH");
            return Language.ENGLISH;
        }

        if (containsEthiopicScript(text)) {
            logger.debug("Language detected as AMHARIC for input snippet: {}", abbreviate(text));
            return Language.AMHARIC;
        }

        if (containsNorwegianCharacters(text) || containsNorwegianWords(text)) {
            logger.debug("Language detected as NORWEGIAN for input snippet: {}", abbreviate(text));
            return Language.NORWEGIAN;
        }

        logger.debug("Language defaulting to ENGLISH for input snippet: {}", abbreviate(text));
        return Language.ENGLISH;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean containsEthiopicScript(String text) {
        return text.chars().anyMatch(cp -> cp >= ETHIOPIC_BLOCK_START && cp <= ETHIOPIC_BLOCK_END);
    }

    private boolean containsNorwegianCharacters(String text) {
        return text.chars().anyMatch(cp -> NORWEGIAN_SPECIAL_CHARS.indexOf(cp) >= 0);
    }

    /**
     * Splits {@code text} into whole words (on any non-letter boundary) and
     * checks whether any word is in {@link #NORWEGIAN_INDICATOR_WORDS}.
     */
    private boolean containsNorwegianWords(String text) {
        String lower = text.toLowerCase();
        // Split on anything that is not a letter (including æøå via \P{L})
        for (String word : lower.split("[\\P{L}]+")) {
            if (!word.isEmpty() && NORWEGIAN_INDICATOR_WORDS.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** Returns a shortened version of {@code text} for logging purposes. */
    private String abbreviate(String text) {
        return text.length() > 60 ? text.substring(0, 60) + "…" : text;
    }
}

