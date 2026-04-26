package com.pathboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathboot.dto.UserInteractionDto;
import com.pathboot.mapper.UserInteractionMapper;
import com.pathboot.model.UserSessionEntity;
import com.pathboot.repository.UserInteractionRepository;
import com.pathboot.repository.UserSessionRepository;
import com.pathboot.session.UserSessionData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Dedicated service for fire-and-forget persistence operations.
 *
 * <p>Spring's {@code @Async} proxy only intercepts calls made <em>through the proxy</em>
 * (i.e. from another bean). Placing {@code @Async} methods in the same class that calls
 * them (self-invocation) bypasses the proxy and executes on the request thread.
 * This service is the single boundary that guarantees every {@code @Async} method
 * is reached via its Spring proxy.</p>
 *
 * <p>Both {@link #persistInteractionAsync} and {@link #persistSessionAsync} are called
 * from other beans ({@link ChatFacadeService} and {@link com.pathboot.session.UserSessionManager}
 * respectively), ensuring true async execution on a separate thread.</p>
 */
@Service
public class AsyncPersistenceService {

    private static final Logger logger = LogManager.getLogger(AsyncPersistenceService.class);

    private final UserInteractionRepository userInteractionRepository;
    private final UserInteractionMapper     userInteractionMapper;
    private final UserSessionRepository     userSessionRepository;
    /** Spring Boot auto-configures this with JavaTimeModule already registered. */
    private final ObjectMapper              objectMapper;

    public AsyncPersistenceService(UserInteractionRepository userInteractionRepository,
                                   UserInteractionMapper userInteractionMapper,
                                   UserSessionRepository userSessionRepository,
                                   ObjectMapper objectMapper) {
        this.userInteractionRepository = userInteractionRepository;
        this.userInteractionMapper     = userInteractionMapper;
        this.userSessionRepository     = userSessionRepository;
        this.objectMapper              = objectMapper;
    }

    // ─── Interaction ──────────────────────────────────────────────────────────

    /**
     * Persists a completed user interaction to SQLite on a background thread.
     * Non-fatal: any error is logged but does not affect the response already sent.
     *
     * @param dto the interaction data to persist
     */
    @Async
    public void persistInteractionAsync(UserInteractionDto dto) {
        try {
            userInteractionRepository.save(userInteractionMapper.toUserInteraction(dto));
            logger.debug("Interaction persisted for session: {}", dto.sessionId());
        } catch (Exception ex) {
            logger.error("Failed to persist user interaction for session {}: {}",
                    dto.sessionId(), ex.getMessage(), ex);
        }
    }

    // ─── Session ──────────────────────────────────────────────────────────────

    /**
     * Persists the current session state to SQLite on a background thread.
     * Non-fatal: any error is logged but does not affect the response already sent.
     *
     * @param sessionId   the session identifier
     * @param sessionData the current in-memory session state
     */
    @Async
    public void persistSessionAsync(String sessionId, UserSessionData sessionData) {
        try {
            String historyJson = objectMapper.writeValueAsString(
                    sessionData.getConversationHistory());
            UserSessionEntity entity = userSessionRepository
                    .findBySessionId(sessionId)
                    .orElseGet(() -> UserSessionEntity.builder()
                            .sessionId(sessionId)
                            .sessionStart(sessionData.getSessionStartTime())
                            .build());
            entity.setConversationHistoryJson(historyJson);
            entity.setTurnCount(sessionData.getConversationHistory().size());
            userSessionRepository.save(entity);
            logger.debug("Session {} persisted to DB ({} turns)", sessionId, entity.getTurnCount());
        } catch (Exception ex) {
            logger.error("Failed to persist session {}: {}", sessionId, ex.getMessage(), ex);
        }
    }
}
