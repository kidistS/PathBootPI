package com.pathboot.enums;

/**
 * Represents the languages that the application can detect and process.
 */
public enum Language {

    /** English language. */
    ENGLISH("English", "en"),

    /** Amharic language (Ethiopic script). */
    AMHARIC("Amharic", "am"),

    /** Norwegian language (Bokmål). */
    NORWEGIAN("Norwegian", "no"),

    /** Fallback when the language cannot be determined. Treated as English. */
    UNKNOWN("Unknown", "en");

    private final String displayName;

    /** ISO 639-1 language code. */
    private final String isoCode;

    Language(String displayName, String isoCode) {
        this.displayName = displayName;
        this.isoCode = isoCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIsoCode() {
        return isoCode;
    }
}

