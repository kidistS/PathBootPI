package com.pathboot.service.classification;

import com.pathboot.enums.DomainType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DomainClassificationService – Unit Tests")
class DomainClassificationServiceTest {

    private DomainClassificationService service;

    @BeforeEach
    void setUp() {
        service = new DomainClassificationService();
    }

    // ── TAX domain ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("TAX domain classification")
    class TaxDomain {

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            "What is the income tax rate in Norway?",
            "When is the tax return deadline?",
            "How do I get a refund on my tax?",
            "What deductions can I claim on my salary?",
            "How does VAT work in Norway?",
            "How do I file my tax return on Altinn?",
            "What is capital gain tax?",
            "Я хочу знать о skatt i Norge",           // Norwegian keyword embedded
            "Hva er fradrag for pendlere?"            // Norwegian: deduction for commuters
        })
        @DisplayName("English/Norwegian TAX questions should return TAX")
        void taxKeywords_shouldReturnTax(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.TAX);
        }

        @Test
        @DisplayName("Norwegian tax keyword 'skattemelding' should return TAX")
        void norwegianTaxKeyword_skattemelding_shouldReturnTax() {
            assertThat(service.classifyDomain("Når sendes skattemelding?")).isEqualTo(DomainType.TAX);
        }

        @Test
        @DisplayName("Norwegian tax keyword 'trinnskatt' should return TAX")
        void norwegianTaxKeyword_trinnskatt_shouldReturnTax() {
            assertThat(service.classifyDomain("Hva er trinnskatt?")).isEqualTo(DomainType.TAX);
        }
    }

    // ── NAV domain ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("NAV domain classification")
    class NavDomain {

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            "How do I apply for unemployment benefits?",
            "What is sick leave policy in Norway?",
            "How can I get disability allowance?",
            "I need help with parental leave",
            "What is child benefit?",
            "How does social security work?",
            "Dagpenger – what are the requirements?",         // Norwegian keyword
            "Jeg er sykmeldt, hva gjør jeg?"                 // Norwegian: I am on sick leave
        })
        @DisplayName("English/Norwegian NAV questions should return NAV")
        void navKeywords_shouldReturnNav(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.NAV);
        }

        @Test
        @DisplayName("Norwegian NAV keyword 'foreldrepenger' should return NAV")
        void norwegianNavKeyword_foreldrepenger_shouldReturnNav() {
            assertThat(service.classifyDomain("Hvor lenge varer foreldrepenger?")).isEqualTo(DomainType.NAV);
        }

        @Test
        @DisplayName("Keyword 'nav' alone should return NAV")
        void keywordNav_alone_shouldReturnNav() {
            assertThat(service.classifyDomain("What does NAV do?")).isEqualTo(DomainType.NAV);
        }
    }

    // ── IMMIGRATION domain ────────────────────────────────────────────────────

    @Nested
    @DisplayName("IMMIGRATION domain classification")
    class ImmigrationDomain {

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            "How do I apply for a work permit in Norway?",
            "What is a residence permit?",
            "I need a visa to travel to Norway",
            "What are the asylum procedures?",
            "Can a refugee get citizenship?",
            "What documents does UDI require?",
            "How do I apply for a d-number?",
            "Hva er kravene for oppholdstillatelse?",         // Norwegian: residence permit requirements
            "Søk om familiegjenforening"                      // Norwegian: family reunion
        })
        @DisplayName("English/Norwegian IMMIGRATION questions should return IMMIGRATION")
        void immigrationKeywords_shouldReturnImmigration(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.IMMIGRATION);
        }

        @Test
        @DisplayName("Norwegian immigration keyword 'statsborgerskap' should return IMMIGRATION")
        void norwegianImmigrationKeyword_statsborgerskap_shouldReturnImmigration() {
            assertThat(service.classifyDomain("Hvordan søker jeg om norsk statsborgerskap?"))
                    .isEqualTo(DomainType.IMMIGRATION);
        }
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null input should return UNKNOWN")
        void nullInput_shouldReturnUnknown() {
            assertThat(service.classifyDomain(null)).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("blank input should return UNKNOWN")
        void blankInput_shouldReturnUnknown() {
            assertThat(service.classifyDomain("   ")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("empty string should return UNKNOWN")
        void emptyString_shouldReturnUnknown() {
            assertThat(service.classifyDomain("")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("unrelated text with no domain keywords should return UNKNOWN")
        void unrelatedText_shouldReturnUnknown() {
            assertThat(service.classifyDomain("The weather in Oslo is nice today")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("tie between TAX and NAV keywords should return UNKNOWN")
        void keywordTie_taxAndNav_shouldReturnUnknown() {
            // "tax" and "nav" each hit once → tie → UNKNOWN
            assertThat(service.classifyDomain("I have a question about tax and nav")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("tie between all three domains should return UNKNOWN")
        void keywordTie_allThreeDomains_shouldReturnUnknown() {
            // one keyword from each domain → three-way tie → UNKNOWN
            assertThat(service.classifyDomain("tax nav visa")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("numeric-only input should return UNKNOWN")
        void numericOnlyInput_shouldReturnUnknown() {
            assertThat(service.classifyDomain("12345")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("punctuation-only input should return UNKNOWN")
        void punctuationOnlyInput_shouldReturnUnknown() {
            assertThat(service.classifyDomain("... ??? !!!")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("very long text with majority TAX keywords should return TAX")
        void veryLongText_withTaxKeywords_shouldReturnTax() {
            String text = "I need to understand the income tax bracket. " .repeat(5)
                    + "What is the VAT rate and salary deduction for pension?";
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.TAX);
        }

        @Test
        @DisplayName("case-insensitive matching – UPPERCASE keywords should match")
        void caseInsensitive_uppercaseKeywords_shouldMatch() {
            assertThat(service.classifyDomain("WHAT IS TAX RETURN DEADLINE?")).isEqualTo(DomainType.TAX);
        }

        @Test
        @DisplayName("single character input should return UNKNOWN")
        void singleCharacterInput_shouldReturnUnknown() {
            assertThat(service.classifyDomain("a")).isEqualTo(DomainType.UNKNOWN);
        }
    }
}

