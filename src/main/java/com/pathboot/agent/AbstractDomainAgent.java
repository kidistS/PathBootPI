package com.pathboot.agent;

import com.pathboot.config.CacheConfig;
import com.pathboot.enums.Language;
import com.pathboot.exception.LlmCommunicationException;
import com.pathboot.service.rag.RagGroundingService;
import com.pathboot.util.PromptBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;

/** Abstract base for domain agents - Template Method pattern. */
public abstract class AbstractDomainAgent implements DomainAgent {

    private static final Logger logger = LogManager.getLogger(AbstractDomainAgent.class);

    protected final ChatClient ollamaChatClient;
    protected final RagGroundingService ragGroundingService;
    protected final PromptBuilder promptBuilder;

    protected AbstractDomainAgent(ChatClient ollamaChatClient,
                                   RagGroundingService ragGroundingService,
                                   PromptBuilder promptBuilder) {
        this.ollamaChatClient    = ollamaChatClient;
        this.ragGroundingService = ragGroundingService;
        this.promptBuilder       = promptBuilder;
    }

    @Override
    @Cacheable(
        value = CacheConfig.DOMAIN_ANSWERS_CACHE,
        key  = "#root.target.getDomainType().name() + '_' + #userLanguage.name() + '_' + #userQuestion"
    )
    public String processUserQuestion(String userQuestion, String sessionId, Language userLanguage) {
        logger.info("[{}] session={} lang={}: {}", getDomainType(), sessionId, userLanguage, abbreviate(userQuestion));

        String groundingContext = ragGroundingService.findRelevantContext(
                userQuestion, getDomainType(), getGroundingFilePath());
        String systemPrompt = (userLanguage == Language.NORWEGIAN)
                ? promptBuilder.buildLanguageAwareDomainSystemPrompt(getDomainDisplayName(), userLanguage, groundingContext)
                : promptBuilder.buildDomainSystemPrompt(getDomainDisplayName(), groundingContext);
        String response = callLlm(systemPrompt, userQuestion);
        logger.info("[{}] Response generated for session {}", getDomainType(), sessionId);

        return response;
    }

    protected abstract String getGroundingFilePath();
    protected abstract String getDomainDisplayName();

    private String callLlm(String systemPrompt, String userMessage) {
        try {
            String response = ollamaChatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            if (response == null || response.isBlank()) {
                throw new LlmCommunicationException("LLM returned empty response for domain: " + getDomainType());
            }
            return response.trim();
        } catch (LlmCommunicationException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("[{}] LLM call failed: {}", getDomainType(), ex.getMessage(), ex);
            throw new LlmCommunicationException("Failed to get LLM response for domain " + getDomainType(), ex);
        }
    }

    private String abbreviate(String text) {
        return text != null && text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }
}