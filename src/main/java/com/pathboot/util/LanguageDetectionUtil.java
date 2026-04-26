package com.pathboot.util;

import com.pathboot.enums.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Utility for detecting the language of a given text string.
 *
 * <h3>Detection strategy</h3>
 * <ol>
 *   <li><b>Amharic</b> – presence of characters in the Ethiopic Unicode block (U+1200–U+137F).</li>
 *   <li><b>Norwegian</b> – presence of Norwegian-specific characters: æ, ø, å (upper/lower case).</li>
 *   <li><b>English</b> – default when neither of the above heuristics trigger.</li>
 * </ol>
 *
 * <p>This is intentionally lightweight and works without a third-party library.
 * It can be replaced with a proper language-identification library if needed.</p>
 */
@Component
public class LanguageDetectionUtil {

    private static final Logger logger = LogManager.getLogger(LanguageDetectionUtil.class);

    // Unicode block boundaries for Ethiopic (Amharic) script
    private static final int ETHIOPIC_BLOCK_START = 0x1200;
    private static final int ETHIOPIC_BLOCK_END   = 0x137F;

    // Norwegian-specific characters
    private static final String NORWEGIAN_SPECIAL_CHARS = "æøåÆØÅ";

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

        if (containsNorwegianCharacters(text)) {
            logger.debug("Language detected as NORWEGIAN for input snippet: {}", abbreviate(text));
            return Language.NORWEGIAN;
        }

        logger.debug("Language defaulting to ENGLISH for input snippet: {}", abbreviate(text));
        return Language.ENGLISH;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private boolean containsEthiopicScript(String text) {
        return text.chars().anyMatch(codePoint ->
                codePoint >= ETHIOPIC_BLOCK_START && codePoint <= ETHIOPIC_BLOCK_END);
    }

    private boolean containsNorwegianCharacters(String text) {
        return text.chars().anyMatch(codePoint ->
                NORWEGIAN_SPECIAL_CHARS.indexOf(codePoint) >= 0);
    }

    /** Returns a shortened version of {@code text} for logging purposes. */
    private String abbreviate(String text) {
        return text.length() > 60 ? text.substring(0, 60) + "…" : text;
    }
}

