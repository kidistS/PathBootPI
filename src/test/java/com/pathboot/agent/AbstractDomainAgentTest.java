package com.pathboot.agent;

import com.pathboot.enums.DomainType;
import com.pathboot.enums.Language;
import com.pathboot.exception.LlmCommunicationException;
import com.pathboot.service.rag.RagGroundingService;
import com.pathboot.util.PathBootConstants;
import com.pathboot.util.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractDomainAgent – Unit Tests (via TaxAgent stub)")
class AbstractDomainAgentTest {

    /** Deep stubs allow fluent ChatClient chain to be mocked without manual chaining. */
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock private RagGroundingService ragGroundingService;
    @Mock private PromptBuilder        promptBuilder;

    /** Minimal concrete subclass used to test AbstractDomainAgent behaviour. */
    private static class TestAgent extends AbstractDomainAgent {
        TestAgent(ChatClient chatClient, RagGroundingService rag, PromptBuilder pb) {
            super(chatClient, rag, pb);
        }
        @Override public DomainType getDomainType()       { return DomainType.TAX; }
        @Override protected String getGroundingFilePath() { return PathBootConstants.TAX_GROUNDING_FILE; }
        @Override protected String getDomainDisplayName() { return "Test Domain"; }
    }

    private TestAgent agent;

    private static final String SESSION_ID    = "session-abc";
    private static final String QUESTION      = "What is the income tax rate?";
    private static final String RAG_CONTEXT   = "Income tax rate is 22%.";
    private static final String SYSTEM_PROMPT = "You are a tax assistant. Context: " + RAG_CONTEXT;
    private static final String LLM_RESPONSE  = "The income tax rate is 22%.";

    @BeforeEach
    void setUp() {
        agent = new TestAgent(chatClient, ragGroundingService, promptBuilder);
    }

    // ── Happy-path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("processUserQuestion – happy path")
    class HappyPath {

        @Test
        @DisplayName("English question → calls RAG, builds standard prompt, calls LLM, returns answer")
        void englishQuestion_shouldCallRagThenLlm_andReturnAnswer() {
            when(ragGroundingService.findRelevantContext(QUESTION, DomainType.TAX,
                    PathBootConstants.TAX_GROUNDING_FILE)).thenReturn(RAG_CONTEXT);
            when(promptBuilder.buildDomainSystemPrompt("Test Domain", RAG_CONTEXT))
                    .thenReturn(SYSTEM_PROMPT);
            when(chatClient.prompt().system(SYSTEM_PROMPT).user(QUESTION).call().content())
                    .thenReturn(LLM_RESPONSE);

            String result = agent.processUserQuestion(QUESTION, SESSION_ID, Language.ENGLISH);

            assertThat(result).isEqualTo(LLM_RESPONSE);
            verify(ragGroundingService, times(1))
                    .findRelevantContext(QUESTION, DomainType.TAX, PathBootConstants.TAX_GROUNDING_FILE);
            verify(promptBuilder, times(1)).buildDomainSystemPrompt("Test Domain", RAG_CONTEXT);
        }

        @Test
        @DisplayName("Norwegian question → uses language-aware prompt builder")
        void norwegianQuestion_shouldUseLangAwarePromptBuilder() {
            when(ragGroundingService.findRelevantContext(QUESTION, DomainType.TAX,
                    PathBootConstants.TAX_GROUNDING_FILE)).thenReturn(RAG_CONTEXT);
            when(promptBuilder.buildLanguageAwareDomainSystemPrompt("Test Domain", Language.NORWEGIAN, RAG_CONTEXT))
                    .thenReturn(SYSTEM_PROMPT);
            when(chatClient.prompt().system(SYSTEM_PROMPT).user(QUESTION).call().content())
                    .thenReturn(LLM_RESPONSE);

            String result = agent.processUserQuestion(QUESTION, SESSION_ID, Language.NORWEGIAN);

            assertThat(result).isEqualTo(LLM_RESPONSE);
            verify(promptBuilder, times(1))
                    .buildLanguageAwareDomainSystemPrompt("Test Domain", Language.NORWEGIAN, RAG_CONTEXT);
            verify(promptBuilder, never()).buildDomainSystemPrompt(anyString(), anyString());
        }

        @Test
        @DisplayName("LLM response is trimmed before returning")
        void llmResponseWithWhitespace_shouldBeTrimmed() {
            when(ragGroundingService.findRelevantContext(any(), any(), any())).thenReturn(RAG_CONTEXT);
            when(promptBuilder.buildDomainSystemPrompt(any(), any())).thenReturn(SYSTEM_PROMPT);
            when(chatClient.prompt().system(any()).user(any()).call().content())
                    .thenReturn("   " + LLM_RESPONSE + "\n  ");

            String result = agent.processUserQuestion(QUESTION, SESSION_ID, Language.ENGLISH);

            assertThat(result).isEqualTo(LLM_RESPONSE);
        }
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("processUserQuestion – error cases")
    class ErrorCases {

        @Test
        @DisplayName("LLM returns null → throws LlmCommunicationException")
        void llmReturnsNull_shouldThrowLlmCommunicationException() {
            when(ragGroundingService.findRelevantContext(any(), any(), any())).thenReturn(RAG_CONTEXT);
            when(promptBuilder.buildDomainSystemPrompt(any(), any())).thenReturn(SYSTEM_PROMPT);
            when(chatClient.prompt().system(any()).user(any()).call().content()).thenReturn(null);

            assertThatThrownBy(() -> agent.processUserQuestion(QUESTION, SESSION_ID, Language.ENGLISH))
                    .isInstanceOf(LlmCommunicationException.class)
                    .hasMessageContaining("empty response");
        }

        @Test
        @DisplayName("LLM returns blank string → throws LlmCommunicationException")
        void llmReturnsBlankString_shouldThrowLlmCommunicationException() {
            when(ragGroundingService.findRelevantContext(any(), any(), any())).thenReturn(RAG_CONTEXT);
            when(promptBuilder.buildDomainSystemPrompt(any(), any())).thenReturn(SYSTEM_PROMPT);
            when(chatClient.prompt().system(any()).user(any()).call().content()).thenReturn("   ");

            assertThatThrownBy(() -> agent.processUserQuestion(QUESTION, SESSION_ID, Language.ENGLISH))
                    .isInstanceOf(LlmCommunicationException.class)
                    .hasMessageContaining("empty response");
        }

        @Test
        @DisplayName("LLM throws runtime exception → wrapped in LlmCommunicationException")
        void llmThrowsRuntimeException_shouldWrapInLlmCommunicationException() {
            when(ragGroundingService.findRelevantContext(any(), any(), any())).thenReturn(RAG_CONTEXT);
            when(promptBuilder.buildDomainSystemPrompt(any(), any())).thenReturn(SYSTEM_PROMPT);
            when(chatClient.prompt().system(any()).user(any()).call().content())
                    .thenThrow(new RuntimeException("Ollama connection refused"));

            assertThatThrownBy(() -> agent.processUserQuestion(QUESTION, SESSION_ID, Language.ENGLISH))
                    .isInstanceOf(LlmCommunicationException.class)
                    .hasMessageContaining("Failed to get LLM response");
        }

        @Test
        @DisplayName("LlmCommunicationException from inner code is rethrown unchanged")
        void llmCommunicationExceptionFromInner_shouldBeRethrownUnchanged() {
            when(ragGroundingService.findRelevantContext(any(), any(), any())).thenReturn(RAG_CONTEXT);
            when(promptBuilder.buildDomainSystemPrompt(any(), any())).thenReturn(SYSTEM_PROMPT);
            LlmCommunicationException original = new LlmCommunicationException("original error");
            when(chatClient.prompt().system(any()).user(any()).call().content()).thenThrow(original);

            assertThatThrownBy(() -> agent.processUserQuestion(QUESTION, SESSION_ID, Language.ENGLISH))
                    .isSameAs(original);
        }
    }
}

