package com.pathboot.session;

import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds all data associated with a single user session.
 *
 * <p>Instances are stored in the {@link UserSessionManager} in-memory map.
 * The conversation history is capped at {@code maxHistorySize} to prevent unbounded growth.</p>
 */
@Getter
@ToString
public class UserSessionData {

    private final String sessionId;
    private final LocalDateTime sessionStartTime;
    private final List<SessionTurn> conversationHistory;
    private final int maxHistorySize;

    public UserSessionData(String sessionId, int maxHistorySize) {
        this.sessionId         = sessionId;
        this.sessionStartTime  = LocalDateTime.now();
        this.conversationHistory = new ArrayList<>();
        this.maxHistorySize    = maxHistorySize;
    }

    /**
     * Appends a new turn to the conversation history, evicting the oldest turn when the
     * cap is exceeded.
     *
     * @param userInput       original user message
     * @param systemResponse  system-generated response
     * @param detectedLanguage language of the user input
     * @param detectedDomain  domain classified for the question
     */
    public synchronized void addTurn(String userInput,
                                     String systemResponse,
                                     Language detectedLanguage,
                                     DomainType detectedDomain) {
        if (conversationHistory.size() >= maxHistorySize) {
            conversationHistory.remove(0);
        }
        conversationHistory.add(SessionTurn.builder()
                .userInput(userInput)
                .systemResponse(systemResponse)
                .detectedLanguage(detectedLanguage)
                .detectedDomain(detectedDomain)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Returns an unmodifiable view of the conversation history.
     *
     * @return read-only list of session turns
     */
    public List<SessionTurn> getConversationHistory() {
        return Collections.unmodifiableList(conversationHistory);
    }
}

