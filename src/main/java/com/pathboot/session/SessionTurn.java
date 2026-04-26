package com.pathboot.session;

import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable snapshot of a single turn within a user session.
 *
 * <p>{@code @NoArgsConstructor} is required by Jackson for JSON deserialization
 * when reloading persisted sessions from SQLite.</p>
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SessionTurn {

    private String userInput;
    private String systemResponse;
    private Language detectedLanguage;
    private DomainType detectedDomain;
    private LocalDateTime timestamp;
}

