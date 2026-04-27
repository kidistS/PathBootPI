package com.pathboot.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;
import com.pathboot.model.UserSessionEntity;
import com.pathboot.repository.UserSessionRepository;
import com.pathboot.service.AsyncPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionManager – Unit Tests")
class UserSessionManagerTest {

    @Mock private UserSessionRepository   userSessionRepository;
    @Mock private AsyncPersistenceService asyncPersistenceService;

    private UserSessionManager sessionManager;

    private static final String SESSION_ID     = "test-session-001";
    private static final String USER_INPUT     = "What is tax?";
    private static final String SYSTEM_RESP    = "Tax is a levy.";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        sessionManager = new UserSessionManager(userSessionRepository, asyncPersistenceService, objectMapper);
    }

    // ── getOrCreateSession ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreateSession")
    class GetOrCreateSession {

        @Test
        @DisplayName("null sessionId → creates a new session with a generated UUID")
        void nullSessionId_shouldCreateNewSession() {
            when(userSessionRepository.findBySessionId(anyString())).thenReturn(Optional.empty());

            UserSessionData session = sessionManager.getOrCreateSession(null);

            assertThat(session).isNotNull();
            assertThat(session.getSessionId()).isNotBlank();
        }

        @Test
        @DisplayName("blank sessionId → creates a new session with a generated UUID")
        void blankSessionId_shouldCreateNewSession() {
            when(userSessionRepository.findBySessionId(anyString())).thenReturn(Optional.empty());

            UserSessionData session = sessionManager.getOrCreateSession("   ");

            assertThat(session).isNotNull();
            assertThat(session.getSessionId()).isNotBlank();
        }

        @Test
        @DisplayName("known sessionId in hot tier → returns same instance (no DB call)")
        void knownSessionId_inHotTier_shouldReturnSameInstance_withoutDbCall() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            // First call – creates session
            UserSessionData first = sessionManager.getOrCreateSession(SESSION_ID);
            // Second call – should hit hot-tier cache
            UserSessionData second = sessionManager.getOrCreateSession(SESSION_ID);

            assertThat(second).isSameAs(first);
            // DB should only have been called the very first time
            verify(userSessionRepository, times(1)).findBySessionId(SESSION_ID);
        }

        @Test
        @DisplayName("session not in hot tier but in DB → restores from DB")
        void sessionInDb_shouldBeRestoredFromDb() throws Exception {
            UserSessionEntity entity = UserSessionEntity.builder()
                    .sessionId(SESSION_ID)
                    .conversationHistoryJson("[]")
                    .turnCount(0)
                    .build();
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(entity));

            UserSessionData restored = sessionManager.getOrCreateSession(SESSION_ID);

            assertThat(restored.getSessionId()).isEqualTo(SESSION_ID);
            verify(userSessionRepository, times(1)).findBySessionId(SESSION_ID);
        }

        @Test
        @DisplayName("DB session with conversation history JSON → history is deserialized")
        void dbSessionWithHistory_shouldDeserializeHistory() throws Exception {
            String historyJson = "[{\"userInput\":\"Hello\",\"systemResponse\":\"Hi\"," +
                    "\"detectedLanguage\":\"ENGLISH\",\"detectedDomain\":\"TAX\"," +
                    "\"timestamp\":\"2026-04-26T10:00:00\"}]";
            UserSessionEntity entity = UserSessionEntity.builder()
                    .sessionId(SESSION_ID)
                    .conversationHistoryJson(historyJson)
                    .turnCount(1)
                    .build();
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(entity));

            UserSessionData restored = sessionManager.getOrCreateSession(SESSION_ID);

            assertThat(restored.getConversationHistory()).hasSize(1);
        }

        @Test
        @DisplayName("DB session with corrupt JSON → session still created (empty history)")
        void dbSessionWithCorruptJson_shouldCreateEmptySession() {
            UserSessionEntity entity = UserSessionEntity.builder()
                    .sessionId(SESSION_ID)
                    .conversationHistoryJson("{invalid json}")
                    .build();
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(entity));

            UserSessionData restored = sessionManager.getOrCreateSession(SESSION_ID);

            assertThat(restored).isNotNull();
            assertThat(restored.getConversationHistory()).isEmpty();
        }

        @Test
        @DisplayName("brand new sessionId not in memory or DB → new empty session created")
        void newSessionId_notInMemoryOrDb_shouldCreateNewEmptySession() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

            UserSessionData session = sessionManager.getOrCreateSession(SESSION_ID);

            assertThat(session.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(session.getConversationHistory()).isEmpty();
        }
    }

    // ── recordSessionTurn ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordSessionTurn")
    class RecordSessionTurn {

        @Test
        @DisplayName("records turn in session history")
        void recordTurn_shouldAddTurnToHistory() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            sessionManager.getOrCreateSession(SESSION_ID);

            sessionManager.recordSessionTurn(SESSION_ID, USER_INPUT, SYSTEM_RESP,
                    Language.ENGLISH, DomainType.TAX);

            List<SessionTurn> history = sessionManager.getSessionHistory(SESSION_ID);
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getUserInput()).isEqualTo(USER_INPUT);
            assertThat(history.get(0).getSystemResponse()).isEqualTo(SYSTEM_RESP);
        }

        @Test
        @DisplayName("calls asyncPersistenceService.persistSessionAsync after recording turn")
        void recordTurn_shouldCallAsyncPersistence() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            sessionManager.getOrCreateSession(SESSION_ID);

            sessionManager.recordSessionTurn(SESSION_ID, USER_INPUT, SYSTEM_RESP,
                    Language.ENGLISH, DomainType.TAX);

            verify(asyncPersistenceService, times(1))
                    .persistSessionAsync(eq(SESSION_ID), any(UserSessionData.class));
        }

        @Test
        @DisplayName("multiple turns accumulate in history")
        void multipleRecordTurns_shouldAccumulateInHistory() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            sessionManager.getOrCreateSession(SESSION_ID);

            sessionManager.recordSessionTurn(SESSION_ID, "Q1", "A1", Language.ENGLISH, DomainType.TAX);
            sessionManager.recordSessionTurn(SESSION_ID, "Q2", "A2", Language.NORWEGIAN, DomainType.NAV);
            sessionManager.recordSessionTurn(SESSION_ID, "Q3", "A3", Language.AMHARIC, DomainType.IMMIGRATION);

            assertThat(sessionManager.getSessionHistory(SESSION_ID)).hasSize(3);
        }

        @Test
        @DisplayName("detected language and domain are stored correctly in turn")
        void recordTurn_shouldStoreLanguageAndDomainCorrectly() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            sessionManager.getOrCreateSession(SESSION_ID);

            sessionManager.recordSessionTurn(SESSION_ID, USER_INPUT, SYSTEM_RESP,
                    Language.AMHARIC, DomainType.IMMIGRATION);

            SessionTurn turn = sessionManager.getSessionHistory(SESSION_ID).get(0);
            assertThat(turn.getDetectedLanguage()).isEqualTo(Language.AMHARIC);
            assertThat(turn.getDetectedDomain()).isEqualTo(DomainType.IMMIGRATION);
        }
    }

    // ── getSessionHistory ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSessionHistory")
    class GetSessionHistory {

        @Test
        @DisplayName("known session in hot tier → returns history without DB call")
        void knownSession_inHotTier_shouldReturnHistoryWithoutDbCall() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            sessionManager.getOrCreateSession(SESSION_ID);
            sessionManager.recordSessionTurn(SESSION_ID, USER_INPUT, SYSTEM_RESP,
                    Language.ENGLISH, DomainType.TAX);
            // reset interaction count for verifying no further DB calls
            clearInvocations(userSessionRepository);

            List<SessionTurn> history = sessionManager.getSessionHistory(SESSION_ID);

            assertThat(history).hasSize(1);
            verifyNoInteractions(userSessionRepository);
        }

        @Test
        @DisplayName("unknown sessionId not in memory or DB → returns empty list")
        void unknownSession_shouldReturnEmptyList() {
            when(userSessionRepository.findBySessionId("unknown-id")).thenReturn(Optional.empty());

            List<SessionTurn> history = sessionManager.getSessionHistory("unknown-id");

            assertThat(history).isEmpty();
        }
    }

    // ── clearSession ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("clearSession")
    class ClearSession {

        @Test
        @DisplayName("clearSession removes session from hot tier")
        void clearSession_shouldRemoveFromHotTier() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            sessionManager.getOrCreateSession(SESSION_ID);

            sessionManager.clearSession(SESSION_ID);

            // After clear, history lookup against cold tier should be called
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            List<SessionTurn> history = sessionManager.getSessionHistory(SESSION_ID);
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("clearSession deletes entity from DB if present")
        void clearSession_shouldDeleteEntityFromDb_ifPresent() {
            UserSessionEntity entity = UserSessionEntity.builder()
                    .sessionId(SESSION_ID).conversationHistoryJson("[]").build();
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(entity));

            sessionManager.clearSession(SESSION_ID);

            verify(userSessionRepository, times(1)).delete(entity);
        }

        @Test
        @DisplayName("clearSession with no DB record → does not throw")
        void clearSession_withNoDbRecord_shouldNotThrow() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

            // Should not throw
            sessionManager.clearSession(SESSION_ID);

            verify(userSessionRepository, never()).delete(any());
        }
    }
}

