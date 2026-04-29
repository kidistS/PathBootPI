package com.pathboot.service.classification;

import com.pathboot.enums.DomainType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DomainClassificationService}.
 *
 * <p>Coverage areas:
 * <ul>
 *   <li>TAX  – English and Norwegian keywords from {@code PathBootConstants.TAX_KEYWORDS}</li>
 *   <li>NAV  – English and Norwegian keywords from {@code PathBootConstants.NAV_KEYWORDS}</li>
 *   <li>IMMIGRATION – English and Norwegian keywords from {@code PathBootConstants.IMMIGRATION_KEYWORDS}</li>
 *   <li>Score dominance – one domain clearly outscoring others</li>
 *   <li>Tie-breaking – equal scores → UNKNOWN</li>
 *   <li>Edge cases – null, blank, numeric, punctuation, single-char</li>
 *   <li>Case-insensitivity</li>
 *   <li>Mixed-language input (embedded Norwegian keyword in foreign text)</li>
 * </ul>
 */
@DisplayName("DomainClassificationService – Unit Tests")
class DomainClassificationServiceTest {

    private DomainClassificationService service;

    @BeforeEach
    void setUp() {
        service = new DomainClassificationService();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TAX domain
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TAX domain classification")
    class TaxDomain {

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            // English keywords
            "What is the income tax rate in Norway?",
            "When is the tax return deadline?",
            "How do I get a refund on my tax?",
            "What deductions can I claim on my salary?",
            "How does VAT work in Norway?",
            "How do I file my tax return on Altinn?",
            "What is capital gain tax?",
            "How is withholding tax calculated?",
            "What is a tax card and how do I get one?",
            "What is the wealth tax threshold?",
            "What bracket of income tax do I fall into?",
            "How do pension contributions affect my filing?",
            "Can I deduct my wage expenses?",
            "My payslip shows withholding – is that normal?"
        })
        @DisplayName("English TAX questions should return TAX")
        void englishTaxKeywords_shouldReturnTax(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.TAX);
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            // Norwegian keywords from PathBootConstants.TAX_KEYWORDS
            "Hva er trinnskatt?",
            "Når sendes skattemelding?",
            "Hva er mva-satsen i Norge?",
            "Hvordan fungerer moms på varer?",
            "Hva er skatteoppgjør?",
            "Hvordan søker jeg om skattekort?",
            "Hva er selvangivelse?",
            "Hvor mye er fradrag for pendlere?",
            "Hva er formuesskatt?",
            "Я хочу знать о skatt i Norge"           // foreign text with Norwegian keyword
        })
        @DisplayName("Norwegian TAX keywords should return TAX")
        void norwegianTaxKeywords_shouldReturnTax(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.TAX);
        }

        @ParameterizedTest(name = "score dominance: \"{0}\" → TAX")
        @ValueSource(strings = {
            "income tax deduction refund salary vat",         // 6 TAX hits, 0 others
            "tax bracket withholding pension capital gain"    // 5 TAX hits, 0 others
        })
        @DisplayName("Input with many TAX keywords clearly wins over other domains")
        void manyTaxKeywords_dominanceShouldReturnTax(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.TAX);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAV domain
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("NAV domain classification")
    class NavDomain {

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            // English keywords
            "How do I apply for unemployment benefits?",
            "What is sick leave policy in Norway?",
            "How can I get disability allowance?",
            "I need help with parental leave",
            "What is child benefit?",
            "How does social security work?",
            "What is work assessment allowance?",
            "What does NAV do?",
            "How is welfare administered in Norway?"
        })
        @DisplayName("English NAV questions should return NAV")
        void englishNavKeywords_shouldReturnNav(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.NAV);
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            // Norwegian keywords from PathBootConstants.NAV_KEYWORDS
            "Dagpenger – hva er kravene?",
            "Jeg er sykmeldt, hva gjør jeg?",
            "Hva er sykepenger?",
            "Hva betyr sykmelding?",
            "Har jeg rett til uføretrygd?",
            "Hva er arbeidsavklaringspenger?",
            "Hvordan søker jeg om foreldrepenger?",
            "Hva er kontantstøtte?",
            "Hvem har rett til barnebidrag?",
            "Hva er sosialhjelp?",
            "Er jeg arbeidsledig – hva har jeg krav på?",
            "Hva er tilleggsstønad fra NAV?"
        })
        @DisplayName("Norwegian NAV keywords should return NAV")
        void norwegianNavKeywords_shouldReturnNav(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.NAV);
        }

        @ParameterizedTest(name = "score dominance: \"{0}\" → NAV")
        @ValueSource(strings = {
            "unemployment sick leave disability benefit welfare allowance",   // 6 NAV hits
            "nav dagpenger sykepenger foreldrepenger trygd"                  // 5 NAV hits
        })
        @DisplayName("Input with many NAV keywords clearly wins over other domains")
        void manyNavKeywords_dominanceShouldReturnNav(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.NAV);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // IMMIGRATION domain
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("IMMIGRATION domain classification")
    class ImmigrationDomain {

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            // English keywords
            "How do I apply for a work permit in Norway?",
            "What is a residence permit?",
            "I need a visa to travel to Norway",
            "What are the asylum procedures?",
            "Can a refugee get citizenship?",
            "What documents does UDI require?",
            "How do I apply for a d-number?",
            "What is the d number registration process?",
            "How do I get a national id as a foreigner?",
            "What are the rules for family reunion?",
            "Can I cross the border with my current passport?",
            "What is the process for entry into Norway?"
        })
        @DisplayName("English IMMIGRATION questions should return IMMIGRATION")
        void englishImmigrationKeywords_shouldReturnImmigration(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.IMMIGRATION);
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {
            // Norwegian keywords from PathBootConstants.IMMIGRATION_KEYWORDS
            "Hva er kravene for oppholdstillatelse?",
            "Søk om familiegjenforening",
            "Hvordan søker jeg om norsk statsborgerskap?",
            "Hva er innvandring til Norge?",
            "Jeg søker om arbeidstillatelse",
            "Hva er asylprosessen i Norge?",
            "Er jeg flyktning – hva har jeg rett på?",
            "Hva er d-nummer og hvordan søker jeg?",
            "Hva er grensen for å få permanent opphold?",
            "Hva gjør utlendingsdirektorat?"
        })
        @DisplayName("Norwegian IMMIGRATION keywords should return IMMIGRATION")
        void norwegianImmigrationKeywords_shouldReturnImmigration(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.IMMIGRATION);
        }

        @ParameterizedTest(name = "score dominance: \"{0}\" → IMMIGRATION")
        @ValueSource(strings = {
            "visa residence permit work permit asylum refugee citizenship",   // 6 IMMIGRATION hits
            "udi oppholdstillatelse statsborgerskap familiegjenforening"      // 4 IMMIGRATION hits
        })
        @DisplayName("Input with many IMMIGRATION keywords clearly wins over other domains")
        void manyImmigrationKeywords_dominanceShouldReturnImmigration(String text) {
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.IMMIGRATION);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tie-breaking – equal scores → UNKNOWN
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tie-breaking – equal domain scores should return UNKNOWN")
    class TieBreaking {

        @Test
        @DisplayName("TAX and NAV tied (1 keyword each) → UNKNOWN")
        void taxNavTie_shouldReturnUnknown() {
            assertThat(service.classifyDomain("I have a question about tax and nav")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("TAX and IMMIGRATION tied (1 keyword each) → UNKNOWN")
        void taxImmigrationTie_shouldReturnUnknown() {
            // "tax" = 1 TAX hit, "visa" = 1 IMMIGRATION hit, 0 NAV → tie → UNKNOWN
            assertThat(service.classifyDomain("tax visa")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("NAV and IMMIGRATION tied (1 keyword each) → UNKNOWN")
        void navImmigrationTie_shouldReturnUnknown() {
            // "nav" = 1 NAV hit, "visa" = 1 IMMIGRATION hit, 0 TAX → tie → UNKNOWN
            assertThat(service.classifyDomain("nav visa")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("Three-way tie (1 keyword each) → UNKNOWN")
        void threeWayTie_shouldReturnUnknown() {
            assertThat(service.classifyDomain("tax nav visa")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("TAX wins by a single keyword over NAV after tie-break → TAX")
        void taxWinsByOneOverNav_shouldReturnTax() {
            // "tax" + "income" = 2 TAX hits; "nav" = 1 NAV hit → TAX wins
            assertThat(service.classifyDomain("What is income tax? I heard nav helps too")).isEqualTo(DomainType.TAX);
        }

        @Test
        @DisplayName("IMMIGRATION wins by a single keyword over NAV after tie-break → IMMIGRATION")
        void immigrationWinsByOneOverNav_shouldReturnImmigration() {
            // "visa" + "asylum" = 2 IMMIGRATION hits; "benefit" = 1 NAV hit → IMMIGRATION wins
            assertThat(service.classifyDomain("I need visa and asylum benefit")).isEqualTo(DomainType.IMMIGRATION);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Case-insensitivity
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Case-insensitivity")
    class CaseInsensitivity {

        @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
        @CsvSource({
            "WHAT IS TAX RETURN DEADLINE?,             TAX",
            "DAGPENGER OG SYKEPENGER,                  NAV",
            "VISA RESIDENCE PERMIT ASYLUM,             IMMIGRATION",
            "What Is The INCOME TAX Rate?,             TAX",
            "How Does PARENTAL LEAVE WORK?,            NAV",
            "OPPHOLDSTILLATELSE OG STATSBORGERSKAP,    IMMIGRATION"
        })
        @DisplayName("Uppercased / mixed-case keywords should still match")
        void mixedCaseKeywords_shouldMatch(String input, DomainType expected) {
            assertThat(service.classifyDomain(input)).isEqualTo(expected);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Edge cases
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null input → UNKNOWN")
        void nullInput_shouldReturnUnknown() {
            assertThat(service.classifyDomain(null)).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("blank (whitespace-only) input → UNKNOWN")
        void blankInput_shouldReturnUnknown() {
            assertThat(service.classifyDomain("   ")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("empty string → UNKNOWN")
        void emptyString_shouldReturnUnknown() {
            assertThat(service.classifyDomain("")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("single character → UNKNOWN")
        void singleCharacter_shouldReturnUnknown() {
            assertThat(service.classifyDomain("a")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("numeric-only input → UNKNOWN")
        void numericOnly_shouldReturnUnknown() {
            assertThat(service.classifyDomain("12345")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("punctuation-only input → UNKNOWN")
        void punctuationOnly_shouldReturnUnknown() {
            assertThat(service.classifyDomain("... ??? !!!")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("unrelated text with no domain keywords → UNKNOWN")
        void unrelatedText_shouldReturnUnknown() {
            assertThat(service.classifyDomain("The weather in Oslo is nice today")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("very long text with majority TAX keywords → TAX")
        void veryLongText_withTaxKeywords_shouldReturnTax() {
            String text = "I need to understand the income tax bracket. ".repeat(5)
                    + "What is the VAT rate and salary deduction for pension?";
            assertThat(service.classifyDomain(text)).isEqualTo(DomainType.TAX);
        }

        @Test
        @DisplayName("repeated single domain keyword → that domain (not tie)")
        void repeatedSingleKeyword_shouldReturnDomain() {
            // "visa" appears 4 times – still 1 unique keyword hit but no opposition → IMMIGRATION
            assertThat(service.classifyDomain("visa visa visa visa")).isEqualTo(DomainType.IMMIGRATION);
        }

        @Test
        @DisplayName("Amharic-only script (no Latin keywords) → UNKNOWN")
        void amharicOnlyText_shouldReturnUnknown() {
            // No English or Norwegian keywords present
            assertThat(service.classifyDomain("ወደ ኖርዌይ ለመሄድ ምን ዓይነት ቪዛ ያስፈልጋል")).isEqualTo(DomainType.UNKNOWN);
        }

        @Test
        @DisplayName("question mark at end of keyword phrase does not break matching")
        void trailingQuestionMark_shouldNotBreakMatch() {
            assertThat(service.classifyDomain("What is skatt?")).isEqualTo(DomainType.TAX);
        }

        @Test
        @DisplayName("keyword embedded inside a longer word should still match (contains check)")
        void keywordEmbeddedInWord_shouldMatch() {
            // "taxation" contains "tax"
            assertThat(service.classifyDomain("Norwegian taxation rules explained")).isEqualTo(DomainType.TAX);
        }
    }
}

