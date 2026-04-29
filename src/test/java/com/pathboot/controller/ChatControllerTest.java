package com.pathboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathboot.model.request.ChatRequest;
import com.pathboot.model.response.ChatResponse;
import com.pathboot.service.ChatFacadeService;
import com.pathboot.session.SessionTurn;
import com.pathboot.session.UserSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController – Unit Tests")
class ChatControllerTest {

    @Mock private ChatFacadeService  chatFacadeService;
    @Mock private UserSessionManager userSessionManager;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String VALID_SESSION  = "550e8400-e29b-41d4-a716-446655440010";
    private static final String CHAT_URL       = "/api/v1/chat";
    private static final String HISTORY_URL    = "/api/v1/chat/sessions/{id}/history";
    private static final String SESSION_URL    = "/api/v1/chat/sessions/{id}";

    @BeforeEach
    void setUp() {
        ChatController controller = new ChatController(chatFacadeService, userSessionManager);
        // Standalone setup: no Spring Security, pure unit test for controller logic
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    // ── POST /api/v1/chat ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/chat")
    class SubmitQuestion {

        @Test
        @DisplayName("valid request body returns HTTP 200 with ChatResponse")
        void validRequest_returns200() throws Exception {
            ChatResponse response = ChatResponse.builder()
                    .responseText("Income tax in Norway is 22%.")
                    .detectedLanguage("ENGLISH")
                    .detectedDomain("TAX")
                    .sessionId(VALID_SESSION)
                    .timestamp(LocalDateTime.now())
                    .build();
            when(chatFacadeService.processUserChatRequest(any(ChatRequest.class))).thenReturn(response);

            mockMvc.perform(post(CHAT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest("What is income tax?", null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.responseText").value("Income tax in Norway is 22%."))
                    .andExpect(jsonPath("$.detectedLanguage").value("ENGLISH"))
                    .andExpect(jsonPath("$.detectedDomain").value("TAX"))
                    .andExpect(jsonPath("$.sessionId").value(VALID_SESSION));
        }

        @Test
        @DisplayName("blank userInput returns HTTP 400 (Bean Validation)")
        void blankUserInput_returns400() throws Exception {
            mockMvc.perform(post(CHAT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userInput\":\"\",\"sessionId\":null}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("userInput exceeding 4000 chars returns HTTP 400")
        void tooLongUserInput_returns400() throws Exception {
            String longInput = "a".repeat(4001);
            mockMvc.perform(post(CHAT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest(longInput, null))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("missing request body returns HTTP 400")
        void missingBody_returns400() throws Exception {
            mockMvc.perform(post(CHAT_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("delegates to chatFacadeService.processUserChatRequest exactly once")
        void delegatesToFacadeService() throws Exception {
            when(chatFacadeService.processUserChatRequest(any())).thenReturn(
                    ChatResponse.builder()
                            .responseText("Answer")
                            .detectedLanguage("ENGLISH")
                            .detectedDomain("TAX")
                            .sessionId(VALID_SESSION)
                            .timestamp(LocalDateTime.now())
                            .build());

            mockMvc.perform(post(CHAT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userInput\":\"What is tax?\",\"sessionId\":null}"));

            verify(chatFacadeService, times(1)).processUserChatRequest(any());
        }

        @Test
        @DisplayName("optional sessionId is passed through — request with sessionId returns 200")
        void withSessionId_returns200() throws Exception {
            when(chatFacadeService.processUserChatRequest(any())).thenReturn(
                    ChatResponse.builder()
                            .responseText("Answer")
                            .detectedLanguage("ENGLISH")
                            .detectedDomain("TAX")
                            .sessionId(VALID_SESSION)
                            .timestamp(LocalDateTime.now())
                            .build());

            mockMvc.perform(post(CHAT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChatRequest("What is tax?", VALID_SESSION))))
                    .andExpect(status().isOk());
        }
    }

    // ── GET /api/v1/chat/sessions/{id}/history ────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/chat/sessions/{id}/history")
    class GetHistory {

        @Test
        @DisplayName("returns HTTP 200 with session history list")
        void knownSession_returns200WithHistory() throws Exception {
            when(userSessionManager.getSessionHistory(VALID_SESSION)).thenReturn(List.of());

            mockMvc.perform(get(HISTORY_URL, VALID_SESSION))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("unknown session returns HTTP 200 with empty list (not 404)")
        void unknownSession_returns200WithEmptyList() throws Exception {
            when(userSessionManager.getSessionHistory(anyString())).thenReturn(List.of());

            mockMvc.perform(get(HISTORY_URL, "unknown-session"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("delegates to userSessionManager.getSessionHistory exactly once")
        void delegatesToSessionManager() throws Exception {
            when(userSessionManager.getSessionHistory(VALID_SESSION)).thenReturn(List.of());

            mockMvc.perform(get(HISTORY_URL, VALID_SESSION));

            verify(userSessionManager, times(1)).getSessionHistory(VALID_SESSION);
        }
    }

    // ── DELETE /api/v1/chat/sessions/{id} ─────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/chat/sessions/{id}")
    class ClearSession {

        @Test
        @DisplayName("returns HTTP 204 No Content")
        void clearSession_returns204() throws Exception {
            mockMvc.perform(delete(SESSION_URL, VALID_SESSION))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("delegates to userSessionManager.clearSession exactly once")
        void delegatesToSessionManager() throws Exception {
            mockMvc.perform(delete(SESSION_URL, VALID_SESSION));

            verify(userSessionManager, times(1)).clearSession(VALID_SESSION);
        }

        @Test
        @DisplayName("response body is empty on 204")
        void clearSession_responseBodyIsEmpty() throws Exception {
            mockMvc.perform(delete(SESSION_URL, VALID_SESSION))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }
    }
}

