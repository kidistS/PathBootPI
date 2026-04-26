package com.pathboot.agent.factory;

import com.pathboot.agent.DomainAgent;
import com.pathboot.agent.immigration.ImmigrationAgent;
import com.pathboot.agent.nav.NavAgent;
import com.pathboot.agent.tax.TaxAgent;
import com.pathboot.enums.DomainType;
import com.pathboot.exception.DomainNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentFactory – Unit Tests")
class AgentFactoryTest {

    @Mock private TaxAgent         taxAgent;
    @Mock private NavAgent         navAgent;
    @Mock private ImmigrationAgent immigrationAgent;

    private AgentFactory agentFactory;

    @BeforeEach
    void setUp() {
        agentFactory = new AgentFactory(taxAgent, navAgent, immigrationAgent);
        agentFactory.registerAgents();
    }

    // ── Happy-path lookups ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful agent resolution")
    class SuccessfulResolution {

        @Test
        @DisplayName("getAgentForDomain(TAX) should return TaxAgent")
        void getAgentForDomain_tax_shouldReturnTaxAgent() {
            DomainAgent result = agentFactory.getAgentForDomain(DomainType.TAX);
            assertThat(result).isSameAs(taxAgent);
        }

        @Test
        @DisplayName("getAgentForDomain(NAV) should return NavAgent")
        void getAgentForDomain_nav_shouldReturnNavAgent() {
            DomainAgent result = agentFactory.getAgentForDomain(DomainType.NAV);
            assertThat(result).isSameAs(navAgent);
        }

        @Test
        @DisplayName("getAgentForDomain(IMMIGRATION) should return ImmigrationAgent")
        void getAgentForDomain_immigration_shouldReturnImmigrationAgent() {
            DomainAgent result = agentFactory.getAgentForDomain(DomainType.IMMIGRATION);
            assertThat(result).isSameAs(immigrationAgent);
        }

        @Test
        @DisplayName("each domain resolves a distinct agent instance")
        void eachDomain_shouldResolveDistinctAgent() {
            DomainAgent tax  = agentFactory.getAgentForDomain(DomainType.TAX);
            DomainAgent nav  = agentFactory.getAgentForDomain(DomainType.NAV);
            DomainAgent imm  = agentFactory.getAgentForDomain(DomainType.IMMIGRATION);
            assertThat(tax).isNotSameAs(nav);
            assertThat(nav).isNotSameAs(imm);
            assertThat(tax).isNotSameAs(imm);
        }

        @Test
        @DisplayName("same domain queried twice returns same instance (singleton)")
        void sameDomain_queriedTwice_returnsSameInstance() {
            DomainAgent first  = agentFactory.getAgentForDomain(DomainType.TAX);
            DomainAgent second = agentFactory.getAgentForDomain(DomainType.TAX);
            assertThat(first).isSameAs(second);
        }
    }

    // ── Edge / error cases ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        @DisplayName("getAgentForDomain(UNKNOWN) should throw DomainNotFoundException")
        void getAgentForDomain_unknown_shouldThrowDomainNotFoundException() {
            assertThatThrownBy(() -> agentFactory.getAgentForDomain(DomainType.UNKNOWN))
                    .isInstanceOf(DomainNotFoundException.class)
                    .hasMessageContaining("UNKNOWN");
        }

        @Test
        @DisplayName("getAgentForDomain(null) should throw NullPointerException")
        void getAgentForDomain_null_shouldThrowException() {
            assertThatThrownBy(() -> agentFactory.getAgentForDomain(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("DomainNotFoundException message contains supported domains hint")
        void domainNotFoundException_shouldContainSupportedDomainsHint() {
            assertThatThrownBy(() -> agentFactory.getAgentForDomain(DomainType.UNKNOWN))
                    .isInstanceOf(DomainNotFoundException.class)
                    .hasMessageContaining("Supported domains");
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerAgents")
    class Registration {

        @Test
        @DisplayName("all three domains registered after registerAgents()")
        void allThreeDomains_registeredAfterInit() {
            // If any were missing, getAgentForDomain would throw DomainNotFoundException
            assertThat(agentFactory.getAgentForDomain(DomainType.TAX)).isNotNull();
            assertThat(agentFactory.getAgentForDomain(DomainType.NAV)).isNotNull();
            assertThat(agentFactory.getAgentForDomain(DomainType.IMMIGRATION)).isNotNull();
        }
    }
}

