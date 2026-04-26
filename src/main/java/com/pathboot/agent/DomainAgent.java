package com.pathboot.agent;

import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;

/**
 * Contract for all domain agents.
 *
 * <p>Each implementation handles one domain (Tax, NAV, or Immigration), loads its own
 * grounding context, and delegates response generation to an LLM.</p>
 *
 * <p>Implementations follow the <em>Strategy</em> and <em>Template Method</em> patterns.</p>
 */
public interface DomainAgent {

    /**
     * Processes a question and returns an answer in the same language as the input.
     *
     * <p>When {@code userLanguage} is {@link Language#NORWEGIAN}, the agent instructs
     * the LLM to respond directly in Norwegian, eliminating two translation round-trips.</p>
     *
     * @param userQuestion the user's question (in English or Norwegian)
     * @param sessionId    the current session identifier (for context/logging)
     * @param userLanguage the language of the question
     * @return response in the user's language
     */
    String processUserQuestion(String userQuestion, String sessionId, Language userLanguage);

    /**
     * Convenience overload that assumes the question is in English.
     *
     * @param englishQuestion the user's question already in English
     * @param sessionId       the current session identifier
     * @return English-language response
     */
    default String processUserQuestion(String englishQuestion, String sessionId) {
        return processUserQuestion(englishQuestion, sessionId, Language.ENGLISH);
    }

    /**
     * Returns the domain type this agent handles.
     *
     * @return the {@link DomainType} identifier
     */
    DomainType getDomainType();
}

