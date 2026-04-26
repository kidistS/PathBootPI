package com.pathboot.service.translation;

/**
 * Contract for translation services.
 *
 * <p>Concrete implementations handle specific language pairs using the appropriate model
 * (Ollama/Mistral for Norwegian, NLLB server for Amharic).</p>
 */
public interface TranslationService {

    /**
     * Translates text from the given source language to the given target language.
     *
     * @param text           the text to translate
     * @param sourceLanguage NLLB language code or display name
     * @param targetLanguage NLLB language code or display name
     * @return translated text
     */
    String translate(String text, String sourceLanguage, String targetLanguage);

    /**
     * Returns {@code true} if this service can handle the given source→target language pair.
     *
     * @param sourceLanguage source language code/name
     * @param targetLanguage target language code/name
     * @return capability flag
     */
    boolean canHandleLanguagePair(String sourceLanguage, String targetLanguage);
}

