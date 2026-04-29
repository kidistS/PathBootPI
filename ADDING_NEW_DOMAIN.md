# PathBoot PI – How to Add a New Domain

> **Last updated:** 2026-04-29
> **Example used throughout:** adding an `EDUCATION` domain.
> All 6 changes are listed in the exact order they should be made.

---

## Overview

Adding a new domain requires changes in exactly **6 places**.
The pipeline, controller, session manager, translation layer, and **`AgentFactory`** are
**never touched** — they are all domain-agnostic by design.

> ✅ **No AgentFactory wiring needed.**
> `AgentFactory` injects `List<DomainAgent>` and auto-registers every `@Component` that
> implements `DomainAgent`. Simply creating your new `@Component` agent is enough —
> the factory discovers it automatically on startup.

| Step | File | Type |
|------|------|------|
| 1 | `enums/DomainType.java` | Add enum constant |
| 2 | `util/PathBootConstants.java` | Add keywords + constants |
| 3 | `grounding/<domain>/<domain>-grounding.txt` | New knowledge file |
| 4 | `agent/<domain>/<Domain>Agent.java` | New agent class (`@Component`) |
| 5 | `service/classification/DomainClassificationService.java` | Add score to classifier |
| 6 | Tests | Cover new domain |

---

## Step 1 — `src/main/java/com/pathboot/enums/DomainType.java`

Add one enum constant **before** `UNKNOWN`:

```java
/** Norwegian education (schools, universities, student loans, kindergarten) domain. */
EDUCATION("Education"),

/** Fallback when the domain cannot be determined. */
UNKNOWN("Unknown");
```

---

## Step 2 — `src/main/java/com/pathboot/util/PathBootConstants.java`

Add a keyword array and two string constants alongside the existing domain blocks
(after `IMMIGRATION_KEYWORDS`):

```java
public static final String[] EDUCATION_KEYWORDS = {
    // English
    "education", "school", "university", "college", "student", "study",
    "scholarship", "tuition", "degree", "bachelor", "master", "phd",
    "kindergarten", "primary school", "high school", "student loan",
    "lanekassen", "admission",
    // Norwegian
    "utdanning", "skole", "universitet", "høyskole", "studere", "stipend",
    "studielån", "lånekassen", "barnehage", "videregående", "grunnskole",
    "eksamen", "semester", "opptak"
};

public static final String EDUCATION_GROUNDING_FILE      = "grounding/education/education-grounding.txt";
public static final String EDUCATION_DOMAIN_DISPLAY_NAME = "Norwegian Education (Utdanning)";
```

---

## Step 3 — `src/main/resources/grounding/education/education-grounding.txt` *(new file)*

Create the plain-text knowledge file that RAG will embed and search.
Separate topics with `---` so the chunker splits them into independent chunks:

```
Norwegian Education System
---
Norway offers free public education from primary school through university for residents.
The school system includes: barnehage (kindergarten, ages 1–5), grunnskole (compulsory,
ages 6–16), videregående skole (upper secondary, ages 16–19), and higher education.

Student Loans and Grants – Lånekassen
---
Lånekassen (the Norwegian State Educational Loan Fund) provides financial support.
Students can receive a combination of grants (stipend) and loans. The basic support for
students living away from home is approximately NOK 123,000 per academic year (2024–25).
Up to 40% converts to a grant if you pass your exams. Applications at lanekassen.no.

University Admission – Samordna Opptak
---
Admission to Norwegian universities is coordinated through Samordna Opptak
(samordnaopptak.no) for bachelor programmes. Primary deadline: 15 April each year.
Admission is based on upper secondary grades (karakterpoeng).

Kindergarten – Barnehage
---
All children aged 1–5 have a legal right to a kindergarten place. Main intake: 15 August.
Maximum monthly fee (makspris) is regulated — NOK 2,000 per month in 2024.
Low-income families may be entitled to reduced price or free core hours.

Upper Secondary – Videregående Skole
---
Free and all young people aged 16–24 have a statutory right to three years of upper
secondary education. Choose between general studies (studieforberedende, leads to
university) or vocational programmes (yrkesfag, leads to fagbrev/trade certificate).
Grades range from 1 (lowest) to 6 (highest).

Recognition of Foreign Education – NOKUT
---
NOKUT assesses and recognises foreign higher education qualifications.
Important for immigrants who obtained a degree abroad and need recognition for employment
or further study in Norway. Applications at nokut.no.
```

> ⚠️ **Before the first restart after adding this file:**
> Delete `data/vector-store.json`.
> `RagGroundingService` will detect the missing file and re-embed all domains
> (including the new one) automatically on startup.

---

## Step 4 — `src/main/java/com/pathboot/agent/education/EducationAgent.java` *(new file)*

Create a new package `agent/education/`.
The agent only overrides 3 methods — RAG retrieval, prompt building, LLM call,
and caching are all inherited from `AbstractDomainAgent`.

**`AgentFactory` auto-discovers this bean via Spring's `List<DomainAgent>` injection —
no factory wiring is needed.**

```java
package com.pathboot.agent.education;

import com.pathboot.agent.AbstractDomainAgent;
import com.pathboot.enums.DomainType;
import com.pathboot.service.rag.RagGroundingService;
import com.pathboot.util.PathBootConstants;
import com.pathboot.util.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Domain agent for Norwegian Education questions (schools, universities,
 * student loans via Lånekassen, kindergarten, foreign credential recognition).
 * Uses RAG context from {@code grounding/education/education-grounding.txt}.
 *
 * <p>Registered automatically by {@link com.pathboot.agent.factory.AgentFactory}
 * via Spring's {@code List<DomainAgent>} injection — no factory wiring required.</p>
 */
@Component
public class EducationAgent extends AbstractDomainAgent {

    public EducationAgent(ChatClient ollamaChatClient,
                          RagGroundingService ragGroundingService,
                          PromptBuilder promptBuilder) {
        super(ollamaChatClient, ragGroundingService, promptBuilder);
    }

    @Override public DomainType getDomainType()       { return DomainType.EDUCATION; }
    @Override protected String getGroundingFilePath() { return PathBootConstants.EDUCATION_GROUNDING_FILE; }
    @Override protected String getDomainDisplayName() { return PathBootConstants.EDUCATION_DOMAIN_DISPLAY_NAME; }
}
```

---

## Step 5 — `src/main/java/com/pathboot/service/classification/DomainClassificationService.java`

Three edits — add score variable, update log lines, extend `selectDomainByHighestScore`:

```java
// In classifyDomain() — add after immigrationScore:
int educationScore = countKeywordHits(lowerText, PathBootConstants.EDUCATION_KEYWORDS);

// Update debug log:
logger.debug("Domain scores – TAX: {}, NAV: {}, IMMIGRATION: {}, EDUCATION: {}",
        taxScore, navScore, immigrationScore, educationScore);

// Update info log:
logger.info("Domain classified as: {} (scores: TAX={}, NAV={}, IMMIGRATION={}, EDUCATION={})",
        result, taxScore, navScore, immigrationScore, educationScore);

// Update method call:
DomainType result = selectDomainByHighestScore(taxScore, navScore, immigrationScore, educationScore);

// Replace selectDomainByHighestScore entirely:
private DomainType selectDomainByHighestScore(int taxScore, int navScore,
                                               int immigrationScore, int educationScore) {
    int maxScore = Math.max(taxScore,
                   Math.max(navScore,
                   Math.max(immigrationScore, educationScore)));
    if (maxScore == 0) return DomainType.UNKNOWN;

    int winners = (taxScore         == maxScore ? 1 : 0)
                + (navScore         == maxScore ? 1 : 0)
                + (immigrationScore == maxScore ? 1 : 0)
                + (educationScore   == maxScore ? 1 : 0);
    if (winners > 1) {
        logger.debug("Domain classification tie – returning UNKNOWN");
        return DomainType.UNKNOWN;
    }

    if (taxScore         == maxScore) return DomainType.TAX;
    if (navScore         == maxScore) return DomainType.NAV;
    if (immigrationScore == maxScore) return DomainType.IMMIGRATION;
    return DomainType.EDUCATION;
}
```

---

## Step 6 — Tests

### `src/test/java/com/pathboot/agent/factory/AgentFactoryTest.java`

```java
// Add mock
@Mock private EducationAgent educationAgent;

// Update setUp() — stub getDomainType() and include in the list
@BeforeEach
void setUp() {
    when(taxAgent.getDomainType()).thenReturn(DomainType.TAX);
    when(navAgent.getDomainType()).thenReturn(DomainType.NAV);
    when(immigrationAgent.getDomainType()).thenReturn(DomainType.IMMIGRATION);
    when(educationAgent.getDomainType()).thenReturn(DomainType.EDUCATION);

    agentFactory = new AgentFactory(List.of(taxAgent, navAgent, immigrationAgent, educationAgent));
}

// Add new test in SuccessfulResolution nested class
@Test
@DisplayName("getAgentForDomain(EDUCATION) should return EducationAgent")
void getAgentForDomain_education_shouldReturnEducationAgent() {
    DomainAgent result = agentFactory.getAgentForDomain(DomainType.EDUCATION);
    assertThat(result).isSameAs(educationAgent);
}

// Update the registration test to include EDUCATION
@Test
@DisplayName("all four domains registered after construction")
void allFourDomains_registeredAfterInit() {
    assertThat(agentFactory.getAgentForDomain(DomainType.TAX)).isNotNull();
    assertThat(agentFactory.getAgentForDomain(DomainType.NAV)).isNotNull();
    assertThat(agentFactory.getAgentForDomain(DomainType.IMMIGRATION)).isNotNull();
    assertThat(agentFactory.getAgentForDomain(DomainType.EDUCATION)).isNotNull();
}
```

### `src/test/java/com/pathboot/service/classification/DomainClassificationServiceTest.java`

Add a new `@Nested` class after `ImmigrationDomain`:

```java
@Nested
@DisplayName("EDUCATION domain classification")
class EducationDomain {

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
        "How do I apply to a Norwegian university?",
        "What is the Lånekassen student loan?",
        "How does kindergarten enrollment work in Norway?",
        "Can I get a scholarship to study in Norway?",
        "How is a foreign degree recognised in Norway?",
        "Hva er opptakskravene til videregående?",      // Norwegian: upper secondary admission
        "Hvor mye er studielån fra lånekassen?"         // Norwegian: how much is student loan
    })
    @DisplayName("Education questions should return EDUCATION")
    void educationKeywords_shouldReturnEducation(String text) {
        assertThat(service.classifyDomain(text)).isEqualTo(DomainType.EDUCATION);
    }

    @Test
    @DisplayName("Norwegian keyword 'lånekassen' alone should return EDUCATION")
    void norwegianKeyword_lanekassen_shouldReturnEducation() {
        assertThat(service.classifyDomain("Hva er lånekassen?")).isEqualTo(DomainType.EDUCATION);
    }

    @Test
    @DisplayName("Norwegian keyword 'barnehage' alone should return EDUCATION")
    void norwegianKeyword_barnehage_shouldReturnEducation() {
        assertThat(service.classifyDomain("Barnehageplasser i Oslo")).isEqualTo(DomainType.EDUCATION);
    }
}
```

---

## Files NOT Touched

| File | Why unchanged |
|------|--------------|
| `ChatFacadeService` | Routes to any `DomainType` through `AgentFactory` — domain-agnostic |
| `AbstractDomainAgent` | Template Method handles all domains — no subclass logic needed |
| **`AgentFactory`** | **Auto-discovers new agents via `List<DomainAgent>` — no wiring needed** |
| `RagGroundingService` | Domain is passed as a runtime parameter to every call |
| `TranslationOrchestrationService` | Translation is language-driven, not domain-driven |
| `UserSessionManager` | Sessions store turns for any domain without domain-specific logic |
| `ChatController` | REST layer is fully domain-agnostic |
| `application.yml` | No configuration changes required |

---

## Completion Checklist

```
[ ] 1. DomainType.java               — add NEW_DOMAIN constant before UNKNOWN
[ ] 2. PathBootConstants.java        — add NEW_DOMAIN_KEYWORDS + GROUNDING_FILE + DISPLAY_NAME
[ ] 3. <domain>-grounding.txt        — create knowledge file with --- separated topic sections
[ ] 4. <Domain>Agent.java            — new @Component extending AbstractDomainAgent (3 overrides)
[ ] 5. DomainClassificationService   — new score var + extend selectDomainByHighestScore()
[ ] 6. Tests                         — AgentFactoryTest + DomainClassificationServiceTest
[ ] 7. Delete data/vector-store.json — forces RAG re-embed on next startup
```
