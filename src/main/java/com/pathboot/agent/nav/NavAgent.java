package com.pathboot.agent.nav;

import com.pathboot.agent.AbstractDomainAgent;
import com.pathboot.enums.DomainType;
import com.pathboot.service.rag.RagGroundingService;
import com.pathboot.util.PathBootConstants;
import com.pathboot.util.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** Domain agent for NAV (Norwegian Labour & Welfare) questions. */
@Component
public class NavAgent extends AbstractDomainAgent {
    public NavAgent(ChatClient ollamaChatClient,
                    RagGroundingService ragGroundingService,
                    PromptBuilder promptBuilder) {
        super(ollamaChatClient, ragGroundingService, promptBuilder);
    }
    @Override public DomainType getDomainType()        { return DomainType.NAV; }
    @Override protected String getGroundingFilePath()  { return PathBootConstants.NAV_GROUNDING_FILE; }
    @Override protected String getDomainDisplayName()  { return PathBootConstants.NAV_DOMAIN_DISPLAY_NAME; }
}