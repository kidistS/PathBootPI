package com.pathboot.util;

import com.pathboot.enums.Language;
import org.springframework.stereotype.Component;

/**
 * Builds structured prompts for the LLM models by combining domain context
 * (grounding data) with the user's question.
 *
 * <p>Centralising prompt construction here makes it easy to refine templates
 * without touching agent or service code.</p>
 */
@Component
public class PromptBuilder {

    /**
     * Builds a system prompt for a domain agent (English response expected).
     *
     * @param domainDisplayName human-readable name of the domain
     * @param groundingContext  content loaded from the domain's grounding file
     * @return formatted system prompt string
     */
    public String buildDomainSystemPrompt(String domainDisplayName, String groundingContext) {
        return String.format(PathBootConstants.AGENT_SYSTEM_PROMPT_TEMPLATE,
                domainDisplayName, groundingContext);
    }

    /**
     * Builds a language-aware system prompt instructing the LLM to respond in
     * the user's language.  Use this for Norwegian input so no separate translation
     * call is needed.
     *
     * @param domainDisplayName  human-readable name of the domain
     * @param userLanguage       the language the user wrote in
     * @param groundingContext   content loaded from the domain's grounding file
     * @return formatted system prompt string
     */
    public String buildLanguageAwareDomainSystemPrompt(String domainDisplayName,
                                                        Language userLanguage,
                                                        String groundingContext) {
        String languageDisplayName = toDisplayName(userLanguage);
        return String.format(PathBootConstants.AGENT_SYSTEM_PROMPT_WITH_LANGUAGE_TEMPLATE,
                domainDisplayName, languageDisplayName, languageDisplayName,
                languageDisplayName, groundingContext);
    }

    /**
     * Builds a Norwegian → English translation prompt for Ollama.
     *
     * @param norwegianText text to translate
     * @return prompt string
     */
    public String buildNorwegianToEnglishPrompt(String norwegianText) {
        return String.format(PathBootConstants.NORWEGIAN_TO_ENGLISH_PROMPT, norwegianText);
    }

    /**
     * Builds an English → Norwegian translation prompt for Ollama.
     *
     * @param englishText text to translate
     * @return prompt string
     */
    public String buildEnglishToNorwegianPrompt(String englishText) {
        return String.format(PathBootConstants.ENGLISH_TO_NORWEGIAN_PROMPT, englishText);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private String toDisplayName(Language language) {
        return language.getDisplayName();
    }
}

