package com.pathboot.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;
import com.pathboot.model.UserSessionEntity;
import com.pathboot.repository.UserSessionRepository;
import com.pathboot.service.AsyncPersistenceService;
import com.pathboot.util.PathBootConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user sessions with a two-tier storage strategy:
 *
 * <ol>
 *   <li><b>Hot tier (in-memory)</b> – {@code ConcurrentHashMap} for fast per-request access.</li>
 *   <li><b>Cold tier (SQLite via JPA)</b> – {@link UserSessionRepository} for persistence
 *       across restarts and for sessions evicted from memory.</li>
 * </ol>
 *
 * <p>On {@link #getOrCreateSession}: memory → DB → create new.</p>
 * <p>On {@link #recordSessionTurn}: update memory immediately, persist to DB asynchronously
 * via {@link AsyncPersistenceService} (separate bean ensures Spring's {@code @Async} proxy
 * is honoured).</p>
 */
@Component
public class UserSessionManager {

    private static final Logger logger = LogManager.getLogger(UserSessionManager.class);

    /** Hot-tier: currently active sessions keyed by session UUID. */
    private final Map<String, UserSessionData> activeSessions = new ConcurrentHashMap<>();
    private final UserSessionRepository   userSessionRepository;
    private final AsyncPersistenceService asyncPersistenceService;
    /** Spring Boot auto-configures this with JavaTimeModule already registered. */
    private final ObjectMapper objectMapper;

    public UserSessionManager(UserSessionRepository userSessionRepository,
                              AsyncPersistenceService asyncPersistenceService,
                              ObjectMapper objectMapper) {
        this.userSessionRepository   = userSessionRepository;
        this.asyncPersistenceService = asyncPersistenceService;
        this.objectMapper            = objectMapper;
    }

    /**
     * Returns an existing session or creates a new one.
     * Lookup order: in-memory → SQLite → create new.
     *
     * @param sessionId client-supplied session ID (may be null)
     * @return the resolved or newly created session
     */
    public UserSessionData getOrCreateSession(String sessionId) {
        String resolvedId = resolveSessionId(sessionId);
        // 1. Check hot tier
        if (activeSessions.containsKey(resolvedId)) {
            return activeSessions.get(resolvedId);
        }
        // 2. Check cold tier (DB) – reconnecting returning user after restart
        Optional<UserSessionEntity> stored = userSessionRepository.findBySessionId(resolvedId);
        if (stored.isPresent()) {
            UserSessionData restored = deserializeSession(stored.get());
            activeSessions.put(resolvedId, restored);
            logger.info("Restored session from DB: {}", resolvedId);
            return restored;
        }
        // 3. Create brand-new session
        logger.info("Creating new session: {}", resolvedId);
        UserSessionData newSession = new UserSessionData(resolvedId,
                PathBootConstants.MAX_SESSION_HISTORY_SIZE);
        activeSessions.put(resolvedId, newSession);
        return newSession;
    }

    /**
     * Records a completed interaction turn in the session history (memory + async DB persist).
     */
    public void recordSessionTurn(String sessionId,
                                   String userInput,
                                   String systemResponse,
                                   Language detectedLanguage,
                                   DomainType detectedDomain) {
        UserSessionData session = getOrCreateSession(sessionId);
        session.addTurn(userInput, systemResponse, detectedLanguage, detectedDomain);
        logger.debug("Session {} updated – total turns: {}", sessionId,
                session.getConversationHistory().size());
        // Delegate to separate bean so @Async proxy is not bypassed by self-invocation
        asyncPersistenceService.persistSessionAsync(sessionId, session);
    }

    /** Returns the conversation history for a session. */
    public List<SessionTurn> getSessionHistory(String sessionId) {
        UserSessionData session = activeSessions.get(sessionId);
        if (session != null) {
            return session.getConversationHistory();
        }
        return userSessionRepository.findBySessionId(sessionId)
                .map(entity -> deserializeSession(entity).getConversationHistory())
                .orElse(List.of());
    }

    /** Removes a session from both memory and the database. */
    public void clearSession(String sessionId) {
        activeSessions.remove(sessionId);
        userSessionRepository.findBySessionId(sessionId)
                .ifPresent(userSessionRepository::delete);
        logger.info("Session cleared: {}", sessionId);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private UserSessionData deserializeSession(UserSessionEntity entity) {
        UserSessionData data = new UserSessionData(entity.getSessionId(),
                PathBootConstants.MAX_SESSION_HISTORY_SIZE);
        String json = entity.getConversationHistoryJson();
        if (json != null && !json.isBlank()) {
            try {
                // Explicit type parameter is required: TypeReference captures generic type at runtime.
                // Replacing with <> would cause type erasure and break Jackson deserialization.
                @SuppressWarnings("Convert2Diamond")
                List<SessionTurn> turns = objectMapper.readValue(
                        json, new TypeReference<List<SessionTurn>>() {});
                turns.forEach(turn -> data.addTurn(
                        turn.getUserInput(),
                        turn.getSystemResponse(),
                        turn.getDetectedLanguage(),
                        turn.getDetectedDomain()));
            } catch (Exception ex) {
                logger.warn("Could not deserialize session history for {}: {}",
                        entity.getSessionId(), ex.getMessage());
            }
        }
        return data;
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            String newId = UUID.randomUUID().toString();
            logger.debug("No session ID provided – generated: {}", newId);
            return newId;
        }
        return sessionId;
    }
}
