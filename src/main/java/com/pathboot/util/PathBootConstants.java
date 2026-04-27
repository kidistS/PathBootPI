package com.pathboot.util;


/**
 * Central repository of all application-wide constant literals.
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
        "dagpenger", "sykepenger", "sykmelding", "sykmeldt", "uføre", "arbeidsavklaringspenger",
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

    // ─── UNKNOWN domain clarification messages (one per supported language) ──
    /**
     * Returned as-is when the domain cannot be determined and the user wrote in English.
     * No LLM call is made — this is a static string.
     */
    public static final String CLARIFICATION_MESSAGE_ENGLISH =
            "I'm sorry, I couldn't determine which topic your question relates to. "
            + "I can help with Norwegian Tax (skatt), NAV (welfare and benefits), "
            + "or Immigration (UDI). "
            + "Could you rephrase your question and mention the specific topic you need help with?";

    /**
     * Returned as-is when the domain cannot be determined and the user wrote in Norwegian.
     */
    public static final String CLARIFICATION_MESSAGE_NORWEGIAN =
            "Beklager, jeg kunne ikke avgjøre hvilket tema spørsmålet ditt gjelder. "
            + "Jeg kan hjelpe med norsk skatt, NAV (velferd og ytelser) eller innvandring (UDI). "
            + "Kan du omformulere spørsmålet ditt og nevne det spesifikke temaet?";

    /**
     * Returned as-is when the domain cannot be determined and the user wrote in Amharic.
     * translateFromEnglish is intentionally NOT called for this path.
     */
    public static final String CLARIFICATION_MESSAGE_AMHARIC =
            "ይቅርታ፣ ጥያቄዎ የሚመለከተውን ርዕሰ ጉዳይ መወሰን አልቻልኩም። "
            + "ስለ ኖርዌይ ግብር (skatt)፣ NAV (ደህንነትና ጥቅማጥቅሞች) "
            + "ወይም ስደት (UDI) መርዳት እችላለሁ። "
            + "ጥያቄዎን እንደገና ቀርጸው ስለሚፈልጉት ርዕሰ ጉዳይ ይጠቅሱ?";

    // ─── Grounding file paths (classpath-relative) ───────────────────────────
    public static final String TAX_GROUNDING_FILE         = "grounding/tax/tax-grounding.txt";
    public static final String NAV_GROUNDING_FILE         = "grounding/nav/nav-grounding.txt";
    public static final String IMMIGRATION_GROUNDING_FILE = "grounding/immigration/immigration-grounding.txt";

    // ─── Domain display names ────────────────────────────────────────────────
    public static final String TAX_DOMAIN_DISPLAY_NAME         = "Norwegian Tax (Skatt)";
    public static final String NAV_DOMAIN_DISPLAY_NAME         = "NAV (Norwegian Labour and Welfare Administration)";
    public static final String IMMIGRATION_DOMAIN_DISPLAY_NAME = "Norwegian Immigration (UDI)";

    // ─── Security ────────────────────────────────────────────────────────────
    /** HTTP request header that must carry a valid API key. */
    public static final String API_KEY_HEADER = "X-API-Key";

    // ─── Session ─────────────────────────────────────────────────────────────
    public static final int MAX_SESSION_HISTORY_SIZE = 20;

    // ─── Swagger ─────────────────────────────────────────────────────────────
    public static final String SWAGGER_API_TITLE       = "PathBoot PI – Multilingual Domain Q&A API";
    public static final String SWAGGER_API_DESCRIPTION =
            "REST API for asking Tax, NAV, and Immigration questions in Amharic, English, or Norwegian.";
    public static final String SWAGGER_API_VERSION = "1.0.0";
}