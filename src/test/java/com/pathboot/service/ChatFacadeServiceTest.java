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
import com.pathboot.util.PathBootConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        when(userSessionManager.getOrCreateSession(any())).thenReturn(sessionData);
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

    // ── Amharic pipeline ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Amharic input – translateToEnglish is called before agent")
    void amharicInput_shouldCallTranslateToEnglishBeforeAgent() {
        String amharicInput  = "ወደ ኖርዌይ ለመሄድ ቪዛ ያስፈልጋል?";
        String englishInput  = "Do I need a visa to go to Norway?";
        String agentReply    = "You need a work permit.";
        String amharicReply  = "የሥራ ፈቃድ ያስፈልግዎታል።";

        when(languageDetectionUtil.detectLanguage(amharicInput)).thenReturn(Language.AMHARIC);
        when(translationOrchestrationService.translateToEnglish(amharicInput, Language.AMHARIC))
                .thenReturn(englishInput);
        when(domainClassificationService.classifyDomain(englishInput)).thenReturn(DomainType.IMMIGRATION);
        when(agentFactory.getAgentForDomain(DomainType.IMMIGRATION)).thenReturn(domainAgent);
        when(domainAgent.processUserQuestion(englishInput, SESSION_ID, Language.ENGLISH))
                .thenReturn(agentReply);
        when(translationOrchestrationService.translateFromEnglish(agentReply, Language.AMHARIC))
                .thenReturn(amharicReply);

        ChatRequest request = ChatRequest.builder().userInput(amharicInput).sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);

        verify(translationOrchestrationService, times(1))
                .translateToEnglish(amharicInput, Language.AMHARIC);
        verify(translationOrchestrationService, times(1))
                .translateFromEnglish(agentReply, Language.AMHARIC);
        assertThat(response.getResponseText()).isEqualTo(amharicReply);
        assertThat(response.getDetectedLanguage()).isEqualTo(Language.AMHARIC.name());
    }

    @Test
    @DisplayName("Amharic input – agent is called with ENGLISH language (not AMHARIC)")
    void amharicInput_agentShouldReceiveEnglishLanguage() {
        String amharicInput = "ቀረጥ ምን ያህል ነው?";
        String englishInput = "What is the tax rate?";

        when(languageDetectionUtil.detectLanguage(amharicInput)).thenReturn(Language.AMHARIC);
        when(translationOrchestrationService.translateToEnglish(amharicInput, Language.AMHARIC))
                .thenReturn(englishInput);
        when(domainClassificationService.classifyDomain(englishInput)).thenReturn(DomainType.TAX);
        when(agentFactory.getAgentForDomain(DomainType.TAX)).thenReturn(domainAgent);
        when(domainAgent.processUserQuestion(englishInput, SESSION_ID, Language.ENGLISH))
                .thenReturn(ENGLISH_RESPONSE);
        when(translationOrchestrationService.translateFromEnglish(ENGLISH_RESPONSE, Language.AMHARIC))
                .thenReturn("የቀረጥ መጠን 22% ነው።");

        ChatRequest request = ChatRequest.builder().userInput(amharicInput).sessionId(SESSION_ID).build();
        chatFacadeService.processUserChatRequest(request);

        // Agent must receive ENGLISH, not AMHARIC
        verify(domainAgent, times(1))
                .processUserQuestion(englishInput, SESSION_ID, Language.ENGLISH);
    }

    // ── Norwegian pipeline ────────────────────────────────────────────────────

    @Test
    @DisplayName("Norwegian input – translateToEnglish is NOT called (pass-through)")
    void norwegianInput_shouldNotCallTranslateToEnglish() {
        String norwegianInput = "Hvordan søker jeg om dagpenger?";

        when(languageDetectionUtil.detectLanguage(norwegianInput)).thenReturn(Language.NORWEGIAN);
        when(domainClassificationService.classifyDomain(norwegianInput)).thenReturn(DomainType.NAV);
        when(agentFactory.getAgentForDomain(DomainType.NAV)).thenReturn(domainAgent);
        when(domainAgent.processUserQuestion(norwegianInput, SESSION_ID, Language.NORWEGIAN))
                .thenReturn("Dagpenger søkes via NAV sin nettside.");

        ChatRequest request = ChatRequest.builder().userInput(norwegianInput).sessionId(SESSION_ID).build();
        chatFacadeService.processUserChatRequest(request);

        verify(translationOrchestrationService, never())
                .translateToEnglish(anyString(), any(Language.class));
        verify(translationOrchestrationService, never())
                .translateFromEnglish(anyString(), any(Language.class));
    }

    @Test
    @DisplayName("Norwegian input – agent receives NORWEGIAN language")
    void norwegianInput_agentShouldReceiveNorwegianLanguage() {
        String norwegianInput = "Hva er trinnskatt?";
        String agentReply     = "Trinnskatt er en progressiv skatt.";

        when(languageDetectionUtil.detectLanguage(norwegianInput)).thenReturn(Language.NORWEGIAN);
        when(domainClassificationService.classifyDomain(norwegianInput)).thenReturn(DomainType.TAX);
        when(agentFactory.getAgentForDomain(DomainType.TAX)).thenReturn(domainAgent);
        when(domainAgent.processUserQuestion(norwegianInput, SESSION_ID, Language.NORWEGIAN))
                .thenReturn(agentReply);

        ChatRequest request = ChatRequest.builder().userInput(norwegianInput).sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);

        verify(domainAgent, times(1))
                .processUserQuestion(norwegianInput, SESSION_ID, Language.NORWEGIAN);
        assertThat(response.getResponseText()).isEqualTo(agentReply);
    }

    // ── UNKNOWN domain short-circuit ──────────────────────────────────────────

    @Test
    @DisplayName("UNKNOWN domain (English) – returns English clarification, no agent called")
    void unknownDomain_english_shouldReturnEnglishClarification() {
        when(domainClassificationService.classifyDomain(anyString())).thenReturn(DomainType.UNKNOWN);

        ChatRequest request = ChatRequest.builder().userInput("Tell me a joke").sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);

        verify(agentFactory, never()).getAgentForDomain(any());
        assertThat(response.getDetectedDomain()).isEqualTo(DomainType.UNKNOWN.name());
        assertThat(response.getResponseText()).containsIgnoringCase("tax");
        assertThat(response.getResponseText()).containsIgnoringCase("nav");
        assertThat(response.getResponseText()).containsIgnoringCase("immigration");
    }

    @Test
    @DisplayName("UNKNOWN domain (Norwegian) – returns Norwegian clarification, no agent called")
    void unknownDomain_norwegian_shouldReturnNorwegianClarification() {
        String norwegianInput = "Hva er meningen med livet?";
        when(languageDetectionUtil.detectLanguage(norwegianInput)).thenReturn(Language.NORWEGIAN);
        when(domainClassificationService.classifyDomain(norwegianInput)).thenReturn(DomainType.UNKNOWN);

        ChatRequest request = ChatRequest.builder().userInput(norwegianInput).sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);

        verify(agentFactory, never()).getAgentForDomain(any());
        verify(translationOrchestrationService, never()).translateFromEnglish(anyString(), any());
        assertThat(response.getDetectedDomain()).isEqualTo(DomainType.UNKNOWN.name());
        assertThat(response.getDetectedLanguage()).isEqualTo(Language.NORWEGIAN.name());
        assertThat(response.getResponseText()).contains("NAV");
        assertThat(response.getResponseText()).contains("skatt");
    }

    @Test
    @DisplayName("UNKNOWN domain (Amharic) – returns Amharic clarification, no back-translation needed")
    void unknownDomain_amharic_shouldReturnAmharicClarification() {
        String amharicInput = "ሰላም ነው?";
        String englishInput = "How are you?";
        when(languageDetectionUtil.detectLanguage(amharicInput)).thenReturn(Language.AMHARIC);
        when(translationOrchestrationService.translateToEnglish(amharicInput, Language.AMHARIC))
                .thenReturn(englishInput);
        when(domainClassificationService.classifyDomain(englishInput)).thenReturn(DomainType.UNKNOWN);

        ChatRequest request = ChatRequest.builder().userInput(amharicInput).sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);

        verify(agentFactory, never()).getAgentForDomain(any());
        verify(translationOrchestrationService, never())
                .translateFromEnglish(anyString(), any(Language.class));
        assertThat(response.getDetectedDomain()).isEqualTo(DomainType.UNKNOWN.name());
        assertThat(response.getDetectedLanguage()).isEqualTo(Language.AMHARIC.name());
        assertThat(response.getResponseText()).contains("NAV");
    }

    // ── Null/blank session handling ───────────────────────────────────────────

    @Test
    @DisplayName("null sessionId – new session UUID is created and returned")
    void nullSessionId_shouldGenerateNewSessionId() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(null).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);

        assertThat(response.getSessionId()).isNotBlank();
    }

    // ── Response timestamp ────────────────────────────────────────────────────

    @Test
    @DisplayName("response always has a non-null timestamp")
    void response_shouldAlwaysHaveTimestamp() {
        ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
        ChatResponse response = chatFacadeService.processUserChatRequest(request);

        assertThat(response.getTimestamp()).isNotNull();
    }

    // ── Farewell detection ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Farewell detection – conversation ending")
    class FarewellDetection {

        // ── Short-circuit: no domain classifier, no agent, no persistence ─────

        @Test
        @DisplayName("farewell input – domain classifier is NEVER called")
        void farewell_shouldNotCallDomainClassifier() {
            when(languageDetectionUtil.detectLanguage("bye")).thenReturn(Language.ENGLISH);

            chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput("bye").sessionId(SESSION_ID).build());

            verify(domainClassificationService, never()).classifyDomain(anyString());
        }

        @Test
        @DisplayName("farewell input – agent factory is NEVER called")
        void farewell_shouldNotCallAgentFactory() {
            when(languageDetectionUtil.detectLanguage("goodbye")).thenReturn(Language.ENGLISH);

            chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput("goodbye").sessionId(SESSION_ID).build());

            verify(agentFactory, never()).getAgentForDomain(any());
        }

        @Test
        @DisplayName("farewell input – async persistence is NEVER called")
        void farewell_shouldNotPersistInteraction() {
            when(languageDetectionUtil.detectLanguage("thank you")).thenReturn(Language.ENGLISH);

            chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput("thank you").sessionId(SESSION_ID).build());

            verify(asyncPersistenceService, never()).persistInteractionAsync(any());
        }

        // ── conversationEnded flag ────────────────────────────────────────────

        @Test
        @DisplayName("farewell input – response has conversationEnded=true")
        void farewell_shouldSetConversationEndedTrue() {
            when(languageDetectionUtil.detectLanguage("bye")).thenReturn(Language.ENGLISH);

            ChatResponse response = chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput("bye").sessionId(SESSION_ID).build());

            assertThat(response.isConversationEnded()).isTrue();
        }

        @Test
        @DisplayName("farewell input – detectedDomain is 'FAREWELL'")
        void farewell_shouldSetDomainToFarewell() {
            when(languageDetectionUtil.detectLanguage("goodbye")).thenReturn(Language.ENGLISH);

            ChatResponse response = chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput("goodbye").sessionId(SESSION_ID).build());

            assertThat(response.getDetectedDomain()).isEqualTo("FAREWELL");
        }

        // ── Session is cleared ────────────────────────────────────────────────

        @Test
        @DisplayName("farewell input – session is cleared after responding")
        void farewell_shouldClearSession() {
            when(languageDetectionUtil.detectLanguage("done")).thenReturn(Language.ENGLISH);

            chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput("done").sessionId(SESSION_ID).build());

            verify(userSessionManager, times(1)).clearSession(SESSION_ID);
        }

        // ── English farewell messages ─────────────────────────────────────────

        @ParameterizedTest(name = "[{index}] English farewell: \"{0}\"")
        @ValueSource(strings = {"bye", "goodbye", "thank you", "thanks", "that's all",
                                "farewell", "take care", "have a good day", "done"})
        @DisplayName("English farewell keywords → English farewell message")
        void englishFarewellKeywords_shouldReturnEnglishMessage(String input) {
            when(languageDetectionUtil.detectLanguage(input)).thenReturn(Language.ENGLISH);

            ChatResponse response = chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput(input).sessionId(SESSION_ID).build());

            assertThat(response.getResponseText()).isEqualTo(PathBootConstants.FAREWELL_MESSAGE_ENGLISH);
            assertThat(response.getDetectedLanguage()).isEqualTo(Language.ENGLISH.name());
        }

        // ── Norwegian farewell messages ───────────────────────────────────────

        @ParameterizedTest(name = "[{index}] Norwegian farewell (good detection): \"{0}\"")
        @ValueSource(strings = {"ha det bra", "farvel", "på gjensyn", "vi snakkes"})
        @DisplayName("Norwegian farewell (language detector correct) → Norwegian farewell message")
        void norwegianFarewellWithCorrectDetection_shouldReturnNorwegianMessage(String input) {
            // These phrases are long enough for the detector to identify them correctly
            when(languageDetectionUtil.detectLanguage(input)).thenReturn(Language.NORWEGIAN);

            ChatResponse response = chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput(input).sessionId(SESSION_ID).build());

            assertThat(response.getResponseText()).isEqualTo(PathBootConstants.FAREWELL_MESSAGE_NORWEGIAN);
            assertThat(response.isConversationEnded()).isTrue();
        }

        @ParameterizedTest(name = "[{index}] Norwegian farewell (misdetected as English): \"{0}\"")
        @ValueSource(strings = {"takk", "ha det", "ses", "farvel"})
        @DisplayName("Short Norwegian farewell misdetected as ENGLISH → still returns Norwegian message")
        void norwegianFarewellMisdetectedAsEnglish_shouldStillReturnNorwegianMessage(String input) {
            // Simulate the language detector misclassifying short Norwegian as English
            when(languageDetectionUtil.detectLanguage(input)).thenReturn(Language.ENGLISH);

            ChatResponse response = chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput(input).sessionId(SESSION_ID).build());

            // isNorwegianFarewell() must override the language detector result
            assertThat(response.getResponseText()).isEqualTo(PathBootConstants.FAREWELL_MESSAGE_NORWEGIAN);
            assertThat(response.isConversationEnded()).isTrue();
        }

        // ── Amharic farewell ──────────────────────────────────────────────────

        @Test
        @DisplayName("Amharic farewell translated to English → Amharic farewell message returned")
        void amharicFarewellTranslatedToEnglish_shouldReturnAmharicMessage() {
            String amharicFarewell = "ደህና ሁን"; // "goodbye" in Amharic
            String englishTranslation = "goodbye";

            when(languageDetectionUtil.detectLanguage(amharicFarewell)).thenReturn(Language.AMHARIC);
            when(translationOrchestrationService.translateToEnglish(amharicFarewell, Language.AMHARIC))
                    .thenReturn(englishTranslation);

            ChatResponse response = chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput(amharicFarewell).sessionId(SESSION_ID).build());

            assertThat(response.getResponseText()).isEqualTo(PathBootConstants.FAREWELL_MESSAGE_AMHARIC);
            assertThat(response.getDetectedLanguage()).isEqualTo(Language.AMHARIC.name());
            assertThat(response.isConversationEnded()).isTrue();
            // No back-translation is called for farewell
            verify(translationOrchestrationService, never())
                    .translateFromEnglish(anyString(), any(Language.class));
        }

        // ── Non-farewell inputs must NOT trigger farewell path ────────────────

        @ParameterizedTest(name = "[{index}] Non-farewell: \"{0}\"")
        @ValueSource(strings = {
            "What is tax?",
            "How do I apply for NAV benefits?",
            "I need help with my visa"
        })
        @DisplayName("Normal questions must NOT be detected as farewell")
        void normalQuestions_shouldNotTriggerFarewell(String input) {
            when(languageDetectionUtil.detectLanguage(input)).thenReturn(Language.ENGLISH);
            when(domainClassificationService.classifyDomain(input)).thenReturn(DomainType.TAX);
            when(agentFactory.getAgentForDomain(DomainType.TAX)).thenReturn(domainAgent);
            when(domainAgent.processUserQuestion(anyString(), anyString(), any())).thenReturn("Answer");

            ChatResponse response = chatFacadeService.processUserChatRequest(
                    ChatRequest.builder().userInput(input).sessionId(SESSION_ID).build());

            assertThat(response.isConversationEnded()).isFalse();
            assertThat(response.getDetectedDomain()).isNotEqualTo("FAREWELL");
            // Domain classification MUST still be called
            verify(domainClassificationService, times(1)).classifyDomain(anyString());
        }

        @Test
        @DisplayName("normal response always has conversationEnded=false")
        void normalResponse_shouldHaveConversationEndedFalse() {
            ChatRequest request = ChatRequest.builder().userInput(USER_INPUT).sessionId(SESSION_ID).build();
            ChatResponse response = chatFacadeService.processUserChatRequest(request);

            assertThat(response.isConversationEnded()).isFalse();
        }
    }
}