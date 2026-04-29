package com.pathboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pathboot.dto.UserInteractionDto;
import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;
import com.pathboot.mapper.UserInteractionMapper;
import com.pathboot.model.UserInteraction;
import com.pathboot.model.UserSessionEntity;
import com.pathboot.repository.UserInteractionRepository;
import com.pathboot.repository.UserSessionRepository;
import com.pathboot.session.UserSessionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncPersistenceService – Unit Tests")
class AsyncPersistenceServiceTest {

    @Mock private UserInteractionRepository userInteractionRepository;
    @Mock private UserInteractionMapper     userInteractionMapper;
    @Mock private UserSessionRepository     userSessionRepository;

    private AsyncPersistenceService service;

    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440009";

    @BeforeEach
    void setUp() {
        // Use a real ObjectMapper with JavaTimeModule (same as Spring Boot configures)
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new AsyncPersistenceService(
                userInteractionRepository, userInteractionMapper,
                userSessionRepository, objectMapper);
    }

    // ── persistInteractionAsync ───────────────────────────────────────────────

    @Nested
    @DisplayName("persistInteractionAsync")
    class PersistInteraction {

        private UserInteractionDto buildDto() {
            return new UserInteractionDto(
                    SESSION_ID, "What is tax?", Language.ENGLISH,
                    null, DomainType.TAX, "Tax is a levy.", "Tax is a levy.");
        }

        @Test
        @DisplayName("saves the mapped entity to the repository")
        void callsSave_withMappedEntity() {
            UserInteraction entity = new UserInteraction();
            when(userInteractionMapper.toUserInteraction(any())).thenReturn(entity);

            service.persistInteractionAsync(buildDto());

            verify(userInteractionRepository, times(1)).save(entity);
        }

        @Test
        @DisplayName("exception in repository.save() is caught silently — does not propagate")
        void repositoryException_isCaughtSilently() {
            when(userInteractionMapper.toUserInteraction(any())).thenReturn(new UserInteraction());
            doThrow(new RuntimeException("DB error")).when(userInteractionRepository).save(any());

            assertThatCode(() -> service.persistInteractionAsync(buildDto()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("exception in mapper is caught silently — does not propagate")
        void mapperException_isCaughtSilently() {
            when(userInteractionMapper.toUserInteraction(any()))
                    .thenThrow(new RuntimeException("Mapping error"));

            assertThatCode(() -> service.persistInteractionAsync(buildDto()))
                    .doesNotThrowAnyException();
        }
    }

    // ── persistSessionAsync ───────────────────────────────────────────────────

    @Nested
    @DisplayName("persistSessionAsync")
    class PersistSession {

        private UserSessionData buildSession() {
            UserSessionData data = new UserSessionData(SESSION_ID, 20);
            data.addTurn("Q1", "A1", Language.ENGLISH, DomainType.TAX);
            return data;
        }

        @Test
        @DisplayName("creates a new UserSessionEntity when none exists in the DB")
        void noExistingEntity_createsNewOne() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

            service.persistSessionAsync(SESSION_ID, buildSession());

            verify(userSessionRepository, times(1)).save(any(UserSessionEntity.class));
        }

        @Test
        @DisplayName("updates the existing entity when already in the DB")
        void existingEntity_updatesIt() {
            UserSessionEntity existing = UserSessionEntity.builder()
                    .sessionId(SESSION_ID)
                    .conversationHistoryJson("[]")
                    .turnCount(0)
                    .build();
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(existing));

            service.persistSessionAsync(SESSION_ID, buildSession());

            verify(userSessionRepository, times(1)).save(existing);
            org.assertj.core.api.Assertions.assertThat(existing.getTurnCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("sets turnCount to match the number of turns in the session")
        void turnCount_matchesHistorySize() {
            UserSessionData data = new UserSessionData(SESSION_ID, 20);
            data.addTurn("Q1", "A1", Language.ENGLISH, DomainType.TAX);
            data.addTurn("Q2", "A2", Language.NORWEGIAN, DomainType.NAV);

            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            service.persistSessionAsync(SESSION_ID, data);

            verify(userSessionRepository).save(argThat(entity ->
                    entity.getTurnCount() == 2));
        }

        @Test
        @DisplayName("exception in repository.save() is caught silently — does not propagate")
        void repositoryException_isCaughtSilently() {
            when(userSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
            doThrow(new RuntimeException("DB locked")).when(userSessionRepository).save(any());

            assertThatCode(() -> service.persistSessionAsync(SESSION_ID, buildSession()))
                    .doesNotThrowAnyException();
        }
    }

}

