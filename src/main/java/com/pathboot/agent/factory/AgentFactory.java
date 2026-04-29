package com.pathboot.agent.factory;

import com.pathboot.agent.DomainAgent;
import com.pathboot.enums.DomainType;
import com.pathboot.exception.DomainNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory and registry for domain agents.
 *
 * <p>Implements the <em>Factory</em> and <em>Multiton</em> design patterns:
 * each domain maps to exactly one agent instance, resolved by {@link DomainType} key.
 * New domains can be added by creating a new {@link DomainAgent} implementation —
 * zero changes required here (<em>Open/Closed Principle</em>).</p>
 *
 * <p>Spring auto-discovers every {@link DomainAgent} bean and injects them as a list.
 * The registry is built in the constructor, removing the need for {@code @PostConstruct}
 * and eliminating direct dependencies on concrete agent types.</p>
 */
@Component
public class AgentFactory {

    private static final Logger logger = LogManager.getLogger(AgentFactory.class);

    /** Registry map – populated once at startup; thread-safe for concurrent reads. */
    private final Map<DomainType, DomainAgent> agentRegistry;

    /**
     * Auto-registers every {@link DomainAgent} bean discovered by Spring.
     * Adding a new domain only requires creating a new {@link DomainAgent} bean.
     *
     * @param agents all {@link DomainAgent} implementations available in the context
     */
    public AgentFactory(List<DomainAgent> agents) {
        this.agentRegistry = agents.stream()
                .collect(Collectors.toMap(DomainAgent::getDomainType, Function.identity()));
        logger.info("AgentFactory initialised with {} agents: {}", agentRegistry.size(), agentRegistry.keySet());
    }

    /**
     * Returns the agent registered for the given domain type.
     *
     * @param domainType the domain to look up
     * @return the corresponding {@link DomainAgent}
     * @throws DomainNotFoundException if no agent is registered for the domain
     */
    public DomainAgent getAgentForDomain(DomainType domainType) {
        DomainAgent agent = agentRegistry.get(domainType);
        if (agent == null) {
            logger.error("No agent registered for domain: {}", domainType);
            throw new DomainNotFoundException(
                    "No agent found for domain: " + domainType + ". Supported domains: " + agentRegistry.keySet());
        }
        logger.debug("Resolved agent {} for domain {}", agent.getClass().getSimpleName(), domainType);
        return agent;
    }

}

