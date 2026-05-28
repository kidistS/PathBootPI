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
        "financial assistance", "allowance", "child benefit", "parental leave",
        "social security", "work assessment", "financial social assistance",
        // Norwegian
        "dagpenger", "sykepenger", "sykmelding", "sykmeldt", "uføre", "sosialhjelp",
        "arbeidsavklaringspenger", "barnebidrag", "foreldrepenger", "kontantstøtte",
        "trygd", "arbeidsledig", "tilleggsstønad", "økonomisk sosialhjelp"
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

    // ─── Farewell / goodbye keyword detection ───────────────────────────────

    /**
     * English farewell phrases – matched against the <em>English</em> version of the
     * user's message (i.e. after Amharic→English translation, or the raw input for
     * English/Norwegian users).
     *
     * <p>All entries must be lower-cased; the check lowercases the input before comparison.</p>
     *
     * @see com.pathboot.service.ChatFacadeService#isFarewell(String)
     */
    public static final String[] FAREWELL_KEYWORDS = {
        "bye", "goodbye", "good bye", "good-bye", "see you", "see ya",
        "thank you", "thanks", "that's all", "that is all", "no more questions",
        "no more help", "i'm done", "im done", "i am done", "done", "quit",
        "exit", "farewell", "take care", "have a good day", "have a nice day",
        "ttyl", "talk later", "chat later", "end", "finish", "close"
    };

    /**
     * Norwegian farewell phrases – matched against the <em>original</em> user input
     * (before any language detection or translation) so that short phrases like
     * {@code "takk"} or {@code "ha det"} are never misclassified as English by the
     * language detector.
     *
     * <p>All entries must be lower-cased.</p>
     *
     * @see com.pathboot.service.ChatFacadeService#isNorwegianFarewell(String)
     */
    public static final String[] FAREWELL_KEYWORDS_NORWEGIAN = {
        "ha det", "ha det bra", "takk", "tusen takk", "mange takk", "farvel",
        "adjø", "på gjensyn", "det var alt", "jeg er ferdig", "vi snakkes",
        "god dag", "god natt", "ha en fin dag", "ses"
    };

    // ─── Farewell goodbye messages (one per supported language) ──────────────
    public static final String FAREWELL_MESSAGE_ENGLISH =
            "Thank you for reaching out! 😊 It was my pleasure to assist you. "
            + "If you ever have more questions about Norwegian Tax, NAV, or Immigration, "
            + "don't hesitate to come back. Have a wonderful day! 👋";

    public static final String FAREWELL_MESSAGE_NORWEGIAN =
            "Takk for at du tok kontakt! 😊 Det var en glede å hjelpe deg. "
            + "Hvis du noen gang har flere spørsmål om norsk skatt, NAV eller innvandring, "
            + "er du alltid velkommen tilbake. Ha en fin dag! 👋";

    public static final String FAREWELL_MESSAGE_AMHARIC =
            "ስለተወያዩን እናመሰግናለን! 😊 እርስዎን መርዳት በጣም አስደሳች ነበር። "
            + "ስለ ኖርዌይ ግብር፣ NAV ወይም ስደት ተጨማሪ ጥያቄዎች ካሉዎት፣ "
            + "እንደገና ለመምጣት ነፃነት ይሰማዎ። መልካም ቀን ይሁንልዎ! 👋";

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

    /**
     * Sessions in the in-memory hot tier that have not been accessed for this many hours
     * are evicted by {@link com.pathboot.session.UserSessionManager#evictStaleSessions()}.
     * The DB copy is retained for audit/restore purposes.
     */
    public static final long SESSION_INACTIVITY_TTL_HOURS = 24L;

    // ─── Swagger ─────────────────────────────────────────────────────────────
    public static final String SWAGGER_API_TITLE       = "PathBoot PI – Multilingual Domain Q&A API";
    public static final String SWAGGER_API_DESCRIPTION =
            "REST API for asking Tax, NAV, and Immigration questions in Amharic, English, or Norwegian.";
    public static final String SWAGGER_API_VERSION = "1.0.0";
}