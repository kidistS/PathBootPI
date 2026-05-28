package com.pathboot.session;

import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Holds all data associated with a single user session.
 *
 * <p>Instances are stored in the {@link UserSessionManager} in-memory map.
 * The conversation history is capped at {@code maxHistorySize} to prevent unbounded growth.</p>
 *
 * <p><b>Eviction:</b> {@link #lastAccessedTime} is updated on every {@link #touch()} call.
 * {@link UserSessionManager} uses a scheduled task to evict sessions that have been
 * inactive longer than the configured TTL, preventing an unbounded growth of the
 * in-memory session map.</p>
 */
@Getter
@ToString
public class UserSessionData {

    private final String sessionId;
    private final LocalDateTime sessionStartTime;
    /** Bounded deque – O(1) add at tail and O(1) eviction from head. */
    private final Deque<SessionTurn> history;
    private final int maxHistorySize;

    /**
     * Updated on every interaction. Read by the scheduled eviction task to determine
     * whether the session has been idle long enough to be removed from memory.
     */
    private volatile LocalDateTime lastAccessedTime;

    public UserSessionData(String sessionId, int maxHistorySize) {
        this.sessionId        = sessionId;
        this.sessionStartTime = LocalDateTime.now();
        this.lastAccessedTime = LocalDateTime.now();
        this.history          = new ArrayDeque<>(maxHistorySize);
        this.maxHistorySize   = maxHistorySize;
    }

    /**
     * Updates {@link #lastAccessedTime} to now.
     * Called by {@link UserSessionManager} whenever the session is retrieved or written to.
     */
    public void touch() {
        this.lastAccessedTime = LocalDateTime.now();
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
        if (history.size() >= maxHistorySize) {
            history.pollFirst();   // O(1) – no array shifting
        }
        history.addLast(SessionTurn.builder()
                .userInput(userInput)
                .systemResponse(systemResponse)
                .detectedLanguage(detectedLanguage)
                .detectedDomain(detectedDomain)
                .timestamp(LocalDateTime.now())
                .build());
        touch(); // keep lastAccessedTime fresh for the eviction scheduler
    }

    /**
     * Returns an unmodifiable snapshot of the conversation history in chronological order.
     *
     * @return read-only list of session turns
     */
    public List<SessionTurn> getConversationHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }
}
