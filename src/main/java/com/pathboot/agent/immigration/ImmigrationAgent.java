package com.pathboot.agent.immigration;

import com.pathboot.agent.AbstractDomainAgent;
import com.pathboot.enums.DomainType;
import com.pathboot.service.rag.RagGroundingService;
import com.pathboot.util.PathBootConstants;
import com.pathboot.util.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** Domain agent for Norwegian Immigration (UDI) questions. */
@Component
public class ImmigrationAgent extends AbstractDomainAgent {
    public ImmigrationAgent(ChatClient ollamaChatClient,
                             RagGroundingService ragGroundingService,
                             PromptBuilder promptBuilder) {
        super(ollamaChatClient, ragGroundingService, promptBuilder);
    }
    @Override public DomainType getDomainType()        { return DomainType.IMMIGRATION; }
    @Override protected String getGroundingFilePath()  { return PathBootConstants.IMMIGRATION_GROUNDING_FILE; }
    @Override protected String getDomainDisplayName()  { return PathBootConstants.IMMIGRATION_DOMAIN_DISPLAY_NAME; }
}