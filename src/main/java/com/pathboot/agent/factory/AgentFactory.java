package com.pathboot.agent.factory;

import com.pathboot.agent.DomainAgent;
import com.pathboot.agent.immigration.ImmigrationAgent;
import com.pathboot.agent.nav.NavAgent;
import com.pathboot.agent.tax.TaxAgent;
import com.pathboot.enums.DomainType;
import com.pathboot.exception.DomainNotFoundException;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory and registry for domain agents.
 *
 * <p>Implements the <em>Factory</em> and <em>Multiton</em> design patterns:
 * each domain maps to exactly one agent instance, resolved by {@link DomainType} key.
 * New domains can be added by registering an additional agent without touching
 * existing code (<em>Open/Closed Principle</em>).</p>
 *
 * <p>The agent registry is populated at startup via {@link PostConstruct}.</p>
 */
@Component
public class AgentFactory {

    private static final Logger logger = LogManager.getLogger(AgentFactory.class);

    private final TaxAgent taxAgent;
    private final NavAgent navAgent;
    private final ImmigrationAgent immigrationAgent;

    /** Registry map – populated once at startup; thread-safe for concurrent reads. */
    private final Map<DomainType, DomainAgent> agentRegistry = new ConcurrentHashMap<>();

    public AgentFactory(TaxAgent taxAgent,
                        NavAgent navAgent,
                        ImmigrationAgent immigrationAgent) {
        this.taxAgent         = taxAgent;
        this.navAgent         = navAgent;
        this.immigrationAgent = immigrationAgent;
    }

    /** Registers all domain agents into the internal registry at bean initialisation. */
    @PostConstruct
    public void registerAgents() {
        agentRegistry.put(DomainType.TAX,         taxAgent);
        agentRegistry.put(DomainType.NAV,         navAgent);
        agentRegistry.put(DomainType.IMMIGRATION, immigrationAgent);
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

