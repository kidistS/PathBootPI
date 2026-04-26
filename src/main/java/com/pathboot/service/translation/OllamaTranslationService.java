package com.pathboot.service.translation;

import com.pathboot.enums.Language;
import com.pathboot.exception.TranslationException;
import com.pathboot.util.PathBootConstants;
import com.pathboot.util.PromptBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Translation service that uses Ollama/Mistral for Norwegian ↔ English translations.
 *
 * <p>Acts as an <em>Adapter</em> between the application's translation API and the
 * Ollama LLM backend, hiding the Spring AI-specific call details.</p>
 */
@Service("ollamaTranslationService")
public class OllamaTranslationService implements TranslationService {

    private static final Logger logger = LogManager.getLogger(OllamaTranslationService.class);


    private final ChatClient ollamaChatClient;
    private final PromptBuilder promptBuilder;

    public OllamaTranslationService(ChatClient ollamaChatClient, PromptBuilder promptBuilder) {
        this.ollamaChatClient = ollamaChatClient;
        this.promptBuilder    = promptBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Supported pairs: Norwegian → English and English → Norwegian.</p>
     */
    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        logger.info("Ollama translation: {} → {}", sourceLanguage, targetLanguage);

        String prompt = buildTranslationPrompt(text, sourceLanguage, targetLanguage);

        try {
            String translated = ollamaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (translated == null || translated.isBlank()) {
                throw new TranslationException("Ollama returned an empty translation.");
            }

            logger.debug("Translation result (snippet): {}",
                    translated.length() > 80 ? translated.substring(0, 80) + "…" : translated);
            return translated.trim();

        } catch (TranslationException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Ollama translation failed: {}", ex.getMessage(), ex);
            throw new TranslationException("Ollama translation service error: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles Norwegian ↔ English pairs only.</p>
     */
    @Override
    public boolean canHandleLanguagePair(String sourceLanguage, String targetLanguage) {
        String src = sourceLanguage.toUpperCase();
        String tgt = targetLanguage.toUpperCase();
        return (Language.NORWEGIAN.name().equals(src) && Language.ENGLISH.name().equals(tgt))
                || (Language.ENGLISH.name().equals(src) && Language.NORWEGIAN.name().equals(tgt));
    }

    // ─── Private helper ───────────────────────────────────────────────────────

    private String buildTranslationPrompt(String text, String sourceLanguage, String targetLanguage) {
        String src = sourceLanguage.toUpperCase();
        String tgt = targetLanguage.toUpperCase();

        if (Language.NORWEGIAN.name().equals(src) && Language.ENGLISH.name().equals(tgt)) {
            return promptBuilder.buildNorwegianToEnglishPrompt(text);
        } else if (Language.ENGLISH.name().equals(src) && Language.NORWEGIAN.name().equals(tgt)) {
            return promptBuilder.buildEnglishToNorwegianPrompt(text);
        }
        throw new TranslationException(
                "OllamaTranslationService cannot handle pair: " + sourceLanguage + " → " + targetLanguage);
    }
}

