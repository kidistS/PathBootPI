package com.pathboot.service;
import com.pathboot.agent.DomainAgent;
import com.pathboot.agent.factory.AgentFactory;
import com.pathboot.dto.UserInteractionDto;
import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;
import com.pathboot.model.request.ChatRequest;
import com.pathboot.model.response.ChatResponse;
import com.pathboot.service.classification.DomainClassificationService;
import com.pathboot.service.translation.TranslationOrchestrationService;
import com.pathboot.session.UserSessionData;
import com.pathboot.session.UserSessionManager;
import com.pathboot.util.LanguageDetectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatFacadeService - Unit Tests")
class ChatFacadeServiceTest {
    @Mock private LanguageDetectionUtil languageDetectionUtil;
    @Mock private TranslationOrchestrationService translationOrchestrationService;
    @Mock private DomainClassificationService domainClassificationService;
    @Mock private AgentFactory agentFactory;
    @Mock private AsyncPersistenceService asyncPersistenceService;
    @Mock private UserSessionManager userSessionManager;
    @Mock private DomainAgent domainAgent;
    @InjectMocks
    private ChatFacadeService chatFacadeService;
    private static final String SESSION_ID       = "test-session-facade-001";
    private static final String USER_INPUT       = "What is the tax filing deadline?";
    private static final String ENGLISH_QUESTION = "What is the tax filing deadline?";
    private static final String ENGLISH_RESPONSE = "The tax filing deadline is 30 April.";
    private static final String FINAL_RESPONSE   = "The tax filing deadline is 30 April.";
    @BeforeEach
    void setUpMocks() {
        UserSessionData sessionData = mock(UserSessionData.class);
        when(sessionData.getSessionId()).thenReturn(SESSION_ID);
        when(userSessionManager.getOrCreateSession(anyString())).thenReturn(sessionData);
        when(languageDetectionUtil.detectLanguage(anyString())).thenReturn(Language.ENGLISH);
        when(domainClassificationService.classifyDomain(anyString())).thenReturn(DomainType.TAX);
        when(agentFactory.getAgentForDomain(DomainType.TAX)).thenReturn(domainAgent);
        when(domainAgent.processUserQuestion(anyString(), anyString(), any(Language.class)))
                .thenReturn(ENGLISH_RESPONSE);
        lenient().when(translationOrchestrationService.translateToEnglish(anyString(), any(Language.class)))
                .thenReturn(ENGLISH_QUESTION);
        lenient().when(translationOrchestrationService.translateFromEnglish(anyString(), any(Language.class)))
                .thenReturn(FINAL_RESPONSE);
    }
    @Test
    @DisplayName("processUserChatRequest - should return response with correct text")
    void processUserChatRequest_shouldReturnCorrectResponseText() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);
        assertThat(response).isNotNull();
        assertThat(response.getResponseText()).isEqualTo(FINAL_RESPONSE);
    }
    @Test
    @DisplayName("processUserChatRequest - should include session ID in response")
    void processUserChatRequest_shouldIncludeSessionIdInResponse() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);
        assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
    }
    @Test
    @DisplayName("processUserChatRequest - should include detected domain in response")
    void processUserChatRequest_shouldIncludeDetectedDomainInResponse() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);
        assertThat(response.getDetectedDomain()).isEqualTo(DomainType.TAX.name());
    }
    @Test
    @DisplayName("processUserChatRequest - should call language detection once")
    void processUserChatRequest_shouldCallLanguageDetectionOnce() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        chatFacadeService.processUserChatRequest(request);
        verify(languageDetectionUtil, times(1)).detectLanguage(USER_INPUT);
    }
    @Test
    @DisplayName("processUserChatRequest - should call domain classification once")
    void processUserChatRequest_shouldCallDomainClassificationOnce() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        chatFacadeService.processUserChatRequest(request);
        verify(domainClassificationService, times(1)).classifyDomain(anyString());
    }
    @Test
    @DisplayName("processUserChatRequest - should call agent for the correct domain (3-arg method)")
    void processUserChatRequest_shouldCallAgentForCorrectDomain() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        chatFacadeService.processUserChatRequest(request);
        verify(agentFactory, times(1)).getAgentForDomain(DomainType.TAX);
        verify(domainAgent, times(1)).processUserQuestion(anyString(), anyString(), eq(Language.ENGLISH));
    }
    @Test
    @DisplayName("processUserChatRequest - should fire async interaction persistence")
    void processUserChatRequest_shouldPersistInteraction() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        chatFacadeService.processUserChatRequest(request);
        verify(asyncPersistenceService, times(1)).persistInteractionAsync(any(UserInteractionDto.class));
    }
    @Test
    @DisplayName("processUserChatRequest - should update session history")
    void processUserChatRequest_shouldUpdateSessionHistory() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        chatFacadeService.processUserChatRequest(request);
        verify(userSessionManager, times(1)).recordSessionTurn(
                anyString(), anyString(), anyString(), any(Language.class), any(DomainType.class));
    }
}