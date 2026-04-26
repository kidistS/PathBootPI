package com.pathboot.agent.tax;
import com.pathboot.agent.AbstractDomainAgent;
import com.pathboot.enums.DomainType;
import com.pathboot.service.rag.RagGroundingService;
import com.pathboot.util.PathBootConstants;
import com.pathboot.util.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
/**
 * Domain agent for Norwegian Tax (Skatteetaten) questions.
 * Uses RAG context from {@code grounding/tax/tax-grounding.txt}.
 */
@Component
public class TaxAgent extends AbstractDomainAgent {
    public TaxAgent(ChatClient ollamaChatClient,
                    RagGroundingService ragGroundingService,
                    PromptBuilder promptBuilder) {
        super(ollamaChatClient, ragGroundingService, promptBuilder);
    }
    @Override public DomainType getDomainType()        { return DomainType.TAX; }
    @Override protected String getGroundingFilePath()  { return PathBootConstants.TAX_GROUNDING_FILE; }
    @Override protected String getDomainDisplayName()  { return PathBootConstants.TAX_DOMAIN_DISPLAY_NAME; }
}