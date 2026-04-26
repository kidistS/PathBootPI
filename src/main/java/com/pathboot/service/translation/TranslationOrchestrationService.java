package com.pathboot.service.translation;

import com.pathboot.enums.Language;
import com.pathboot.exception.TranslationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Orchestrates translation by routing each request to the appropriate translation service.
 *
 * <p>Routing rules:</p>
 * <ul>
 *   <li>Amharic ↔ English → {@link NllbTranslationService}</li>
 *   <li>Norwegian ↔ English → {@link OllamaTranslationService}</li>
 *   <li>English input → no translation needed</li>
 * </ul>
 *
 * <p>Implements the <em>Strategy</em> pattern: the correct translation strategy is
 * selected at runtime based on the detected language.</p>
 */
@Service
public class TranslationOrchestrationService {

    private static final Logger logger = LogManager.getLogger(TranslationOrchestrationService.class);

    private final TranslationService ollamaTranslationService;
    private final TranslationService nllbTranslationService;

    public TranslationOrchestrationService(
            @Qualifier("ollamaTranslationService") TranslationService ollamaTranslationService,
            @Qualifier("nllbTranslationService")  TranslationService nllbTranslationService) {
        this.ollamaTranslationService = ollamaTranslationService;
        this.nllbTranslationService   = nllbTranslationService;
    }

    /**
     * Translates the given {@code text} to English if it is not already in English.
     *
     * @param text             the original user text
     * @param detectedLanguage the language of the text
     * @return English version of the text
     */
    public String translateToEnglish(String text, Language detectedLanguage) {
        if (detectedLanguage == Language.ENGLISH || detectedLanguage == Language.UNKNOWN) {
            logger.debug("No translation needed (language: {})", detectedLanguage);
            return text;
        }

        String sourceLang = detectedLanguage.name();
        logger.info("Translating {} → ENGLISH", sourceLang);
        return resolveTranslationService(sourceLang, "ENGLISH")
                .translate(text, sourceLang, "ENGLISH");
    }

    /**
     * Translates an English response back into the user's original language.
     *
     * @param englishText      the English text to translate
     * @param targetLanguage   the language to translate into
     * @return translated text, or the original English if no translation is needed
     */
    public String translateFromEnglish(String englishText, Language targetLanguage) {
        if (targetLanguage == Language.ENGLISH || targetLanguage == Language.UNKNOWN) {
            logger.debug("No reverse translation needed (target: {})", targetLanguage);
            return englishText;
        }

        String tgtLang = targetLanguage.name();
        logger.info("Translating ENGLISH → {}", tgtLang);
        return resolveTranslationService("ENGLISH", tgtLang)
                .translate(englishText, "ENGLISH", tgtLang);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private TranslationService resolveTranslationService(String sourceLang, String targetLang) {
        if (nllbTranslationService.canHandleLanguagePair(sourceLang, targetLang)) {
            logger.debug("Using NLLB translation service for {} → {}", sourceLang, targetLang);
            return nllbTranslationService;
        }
        if (ollamaTranslationService.canHandleLanguagePair(sourceLang, targetLang)) {
            logger.debug("Using Ollama translation service for {} → {}", sourceLang, targetLang);
            return ollamaTranslationService;
        }
        logger.error("No translation service available for {} → {}", sourceLang, targetLang);
        throw new TranslationException(
                "No translation service available for language pair: " + sourceLang + " → " + targetLang);
    }
}

