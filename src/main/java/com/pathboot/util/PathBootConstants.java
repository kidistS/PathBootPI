package com.pathboot.util;

import com.pathboot.enums.DomainType;

/**
 * Central repository of all application-wide constant literals.
 *
 * <p>All magic strings and numbers are declared here as {@code public static final}
 * fields so they can be referenced by name throughout the codebase.</p>
 */
public final class PathBootConstants {

    private PathBootConstants() {
        // Utility class – do not instantiate
    }

    // ─── NLLB language codes ────────────────────────────────────────────────
    public static final String NLLB_AMHARIC_CODE = "amh_Ethi";
    public static final String NLLB_ENGLISH_CODE = "eng_Latn";
    // Norwegian is handled by Ollama/Mistral, not NLLB, so no NLLB code is needed.


    // ─── Norwegian translation prompt templates ──────────────────────────────
    public static final String NORWEGIAN_TO_ENGLISH_PROMPT =
            "Translate the following Norwegian text to English. "
            + "Return only the translated text without any explanation or extra words. "
            + "Text: %s";

    public static final String ENGLISH_TO_NORWEGIAN_PROMPT =
            "Translate the following English text to Norwegian. "
            + "Return only the translated text without any explanation or extra words. "
            + "Text: %s";

    // ─── Agent response system prompt template ───────────────────────────────
    /** Args: domainDisplayName, groundingContext */
    public static final String AGENT_SYSTEM_PROMPT_TEMPLATE =
            "You are a concise assistant for %s in Norway. "
            + "Answer using ONLY the context below. "
            + "Be brief – 3 to 5 sentences maximum. "
            + "If the answer is not in the context, say so in one sentence. "
            + "Context:\n%s";

    /**
     * Language-aware system prompt.
     * Args (5): domainDisplayName, languageDisplayName, languageDisplayName,
     *           languageDisplayName, groundingContext
     */
    public static final String AGENT_SYSTEM_PROMPT_WITH_LANGUAGE_TEMPLATE =
            "You are a concise assistant for %s in Norway. "
            + "The user writes in %s – respond ONLY in %s. "
            + "Answer using ONLY the context below in 3 to 5 sentences maximum. "
            + "If the answer is not in the context, say so in one sentence in %s. "
            + "Context:\n%s";

    // ─── Domain keyword lists (used by keyword-based classifier) ─────────────
    public static final String[] TAX_KEYWORDS = {
        // English
        "tax", "income", "deduction", "refund", "return", "salary", "wage", "payslip",
        "pension", "capital gain", "property tax", "vat", "filing", "withholding",
        "bracket", "wealth", "tax card", "tax return", "altinn",
        // Norwegian
        "skatt", "inntekt", "fradrag", "selvangivelse", "skattekort", "skattemelding",
        "mva", "moms", "trygdeavgift", "formue", "trinnskatt", "skatteoppgjør"
    };

    public static final String[] NAV_KEYWORDS = {
        // English
        "nav", "unemployment", "sick leave", "disability", "benefit", "welfare",
        "allowance", "child benefit", "parental leave", "social security", "work assessment",
        // Norwegian
        "dagpenger", "sykepenger", "sykmelding", "uføre", "arbeidsavklaringspenger",
        "barnebidrag", "foreldrepenger", "kontantstøtte", "trygd", "sosialhjelp",
        "arbeidsledig", "tilleggsstønad"
    };

    public static final String[] IMMIGRATION_KEYWORDS = {
        // English
        "immigration", "visa", "residence permit", "work permit", "asylum", "refugee",
        "citizenship", "passport", "border", "entry", "udi", "family reunion",
        "d-number", "d number", "national id",
        // Norwegian
        "innvandring", "oppholdstillatelse", "arbeidstillatelse", "asyl", "flyktning",
        "statsborgerskap", "grense", "familiegjenforening", "d-nummer", "utlendingsdirektorat"
    };

    // ─── Grounding file paths (classpath-relative) ───────────────────────────
    public static final String TAX_GROUNDING_FILE         = "grounding/tax/tax-grounding.txt";
    public static final String NAV_GROUNDING_FILE         = "grounding/nav/nav-grounding.txt";
    public static final String IMMIGRATION_GROUNDING_FILE = "grounding/immigration/immigration-grounding.txt";

    // ─── Domain display names ────────────────────────────────────────────────
    public static final String TAX_DOMAIN_DISPLAY_NAME         = "Norwegian Tax (Skatt)";
    public static final String NAV_DOMAIN_DISPLAY_NAME         = "NAV (Norwegian Labour and Welfare Administration)";
    public static final String IMMIGRATION_DOMAIN_DISPLAY_NAME = "Norwegian Immigration (UDI)";

    // ─── Session ─────────────────────────────────────────────────────────────
    public static final int MAX_SESSION_HISTORY_SIZE = 20;

    /**
     * Domain used as a fallback when the classifier cannot determine the topic.
     * Tax is the most commonly queried domain, so it is the safest default.
     */
    public static final DomainType DEFAULT_FALLBACK_DOMAIN = DomainType.TAX;

    // ─── Swagger ─────────────────────────────────────────────────────────────
    public static final String SWAGGER_API_TITLE       = "PathBoot PI – Multilingual Domain Q&A API";
    public static final String SWAGGER_API_DESCRIPTION =
            "REST API for asking Tax, NAV, and Immigration questions in English, Amharic, or Norwegian.";
    public static final String SWAGGER_API_VERSION = "1.0.0";
}