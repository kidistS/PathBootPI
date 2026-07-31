# PathBoot PI – How to Add a New Domain

> **Last updated:** 2026-07-31
> **Example below uses `EDUCATION` as the new domain.**

## Before You Start

Make sure the local stack is working first:
- `README.md` for run commands
- `DOCUMENTATION.md` for interaction flow

If runtime services are unavailable (Ollama/NLLB), classification and end-to-end tests may be harder to validate manually.

## Overview

Adding a new domain requires exactly 6 code areas:

1. `enums/DomainType.java`
2. `util/PathBootConstants.java`
3. `src/main/resources/grounding/<domain>/<domain>-grounding.txt`
4. `agent/<domain>/<Domain>Agent.java`
5. `service/classification/DomainClassificationService.java`
6. tests

`AgentFactory` is not manually edited. It auto-discovers `DomainAgent` beans.

## Step 1 – Domain enum

File: `src/main/java/com/pathboot/enums/DomainType.java`

Add your new enum before `UNKNOWN`:

```java
EDUCATION("Education"),
UNKNOWN("Unknown");
```

## Step 2 – Domain constants and keywords

File: `src/main/java/com/pathboot/util/PathBootConstants.java`

Add:
- `EDUCATION_KEYWORDS`
- `EDUCATION_GROUNDING_FILE`
- `EDUCATION_DOMAIN_DISPLAY_NAME`

Include multilingual keywords (English/Norwegian, and any language that should classify reliably).

## Step 3 – Grounding file

File (new): `src/main/resources/grounding/education/education-grounding.txt`

Guidelines:
- plain text only
- use `---` separators between topic blocks
- keep each section factual and concise
- avoid duplicate paragraphs

After adding/updating grounding files, delete:
- `data/vector-store.json`

Then restart backend to regenerate embeddings.

## Step 4 – New domain agent

File (new): `src/main/java/com/pathboot/agent/education/EducationAgent.java`

Requirements:
- class annotated with `@Component`
- extends `AbstractDomainAgent`
- override:
  - `getDomainType()`
  - `getGroundingFilePath()`
  - `getDomainDisplayName()`

`AgentFactory` will discover it automatically.

## Step 5 – Extend classifier

File: `src/main/java/com/pathboot/service/classification/DomainClassificationService.java`

Update classifier to:
- compute score for new keyword set
- include score in logging
- include score in highest-score resolver
- return `UNKNOWN` on tie/zero (existing behavior should remain)

## Step 6 – Tests

Update/add tests:
- `src/test/java/com/pathboot/agent/factory/AgentFactoryTest.java`
- `src/test/java/com/pathboot/service/classification/DomainClassificationServiceTest.java`

Minimum checks:
- factory resolves new domain agent
- classifier maps representative questions to new domain
- tie and unknown behavior remain correct

Run:

```powershell
.\mvnw.cmd test
```

## Files Usually Not Touched

- `service/ChatFacadeService.java`
- `agent/AbstractDomainAgent.java`
- `agent/factory/AgentFactory.java`
- `service/translation/*`
- `controller/ChatController.java`

These stay unchanged because they are already domain-agnostic.

## Completion Checklist

```text
[ ] Add enum in DomainType
[ ] Add keywords/constants in PathBootConstants
[ ] Add grounding file
[ ] Add new domain agent class
[ ] Extend DomainClassificationService scoring
[ ] Extend tests (factory + classification)
[ ] Delete data/vector-store.json and restart
[ ] Run backend tests
```

## Related Docs

- `README.md`
- `DOCUMENTATION.md`
- `PATHBOOT_DOCS.md`
