package com.pathboot.enums;

import lombok.Getter;

/**
 * Represents the supported knowledge domains of the application.
 *
 * <p>Each domain has a dedicated agent, prompt, and data-grounding file.</p>
 */
@Getter
public enum DomainType {

    /** Norwegian tax (Skatteetaten) domain. */
    TAX("Tax"),

    /** NAV – Norwegian Labour and Welfare Administration domain. */
    NAV("NAV"),

    /** Norwegian immigration (UDI) domain. */
    IMMIGRATION("Immigration"),

    /** Fallback when the domain cannot be determined. */
    UNKNOWN("Unknown");

    private final String displayName;

    DomainType(String displayName) {
        this.displayName = displayName;
    }

}

