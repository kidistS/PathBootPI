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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Facade orchestrating the optimised 1-LLM-call pipeline.
 *
 * <p>Async DB persistence is delegated to {@link AsyncPersistenceService} so that
 * Spring's AOP proxy is respected (self-invocation would bypass {@code @Async}).</p>
 */
@Service
public class ChatFacadeService {
    private static final Logger logger = LogManager.getLogger(ChatFacadeService.class);
    private final LanguageDetectionUtil languageDetectionUtil;
    private final TranslationOrchestrationService translationOrchestrationService;
    private final DomainClassificationService domainClassificationService;
    private final AgentFactory agentFactory;
    private final AsyncPersistenceService asyncPersistenceService;
    private final UserSessionManager userSessionManager;

    public ChatFacadeService(LanguageDetectionUtil languageDetectionUtil,
                              TranslationOrchestrationService translationOrchestrationService,
                              DomainClassificationService domainClassificationService,
                              AgentFactory agentFactory,
                              AsyncPersistenceService asyncPersistenceService,
                              UserSessionManager userSessionManager) {
        this.languageDetectionUtil           = languageDetectionUtil;
        this.translationOrchestrationService = translationOrchestrationService;
        this.domainClassificationService     = domainClassificationService;
        this.agentFactory                    = agentFactory;
        this.asyncPersistenceService         = asyncPersistenceService;
        this.userSessionManager              = userSessionManager;
    }

    public ChatResponse processUserChatRequest(ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();
        logger.info("Processing chat request - session: {}", chatRequest.getSessionId());

        UserSessionData session = userSessionManager.getOrCreateSession(chatRequest.getSessionId());
        Language detectedLanguage = languageDetectionUtil.detectLanguage(chatRequest.getUserInput());
        String resolvedSessionId = session.getSessionId();
        logger.info("[Session {}] Detected language: {}", resolvedSessionId, detectedLanguage);

        final String translatedToEnglish;
        final String questionForProcessing;
        if (detectedLanguage == Language.AMHARIC) {
            translatedToEnglish   = translationOrchestrationService
                    .translateToEnglish(chatRequest.getUserInput(), detectedLanguage);
            questionForProcessing = translatedToEnglish;
        } else {
            questionForProcessing = chatRequest.getUserInput();
            translatedToEnglish   = chatRequest.getUserInput();
        }
        DomainType detectedDomain = domainClassificationService.classifyDomain(questionForProcessing);
        logger.info("[Session {}] Detected domain: {}", resolvedSessionId, detectedDomain);

        // ── UNKNOWN domain: short-circuit — no LLM call, no agent lookup ──────
        if (detectedDomain == DomainType.UNKNOWN) {
            logger.warn("[Session {}] Domain could not be determined – returning clarification message",
                    resolvedSessionId);

            final String clarification;
            if (detectedLanguage == Language.AMHARIC) {
                clarification = PathBootConstants.CLARIFICATION_MESSAGE_AMHARIC;
            } else if (detectedLanguage == Language.NORWEGIAN) {
                clarification = PathBootConstants.CLARIFICATION_MESSAGE_NORWEGIAN;
            } else {
                clarification = PathBootConstants.CLARIFICATION_MESSAGE_ENGLISH;
            }
            asyncPersistenceService.persistInteractionAsync(new UserInteractionDto(
                    resolvedSessionId,
                    chatRequest.getUserInput(),
                    detectedLanguage,
                    translatedToEnglish.equals(chatRequest.getUserInput()) ? null : translatedToEnglish,
                    DomainType.UNKNOWN,
                    PathBootConstants.CLARIFICATION_MESSAGE_ENGLISH,
                    clarification));

            userSessionManager.recordSessionTurn(resolvedSessionId, chatRequest.getUserInput(),
                    clarification, detectedLanguage, DomainType.UNKNOWN);

            return ChatResponse.builder()
                    .responseText(clarification)
                    .detectedLanguage(detectedLanguage.name())
                    .detectedDomain(DomainType.UNKNOWN.name())
                    .sessionId(resolvedSessionId)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        DomainAgent domainAgent     = agentFactory.getAgentForDomain(detectedDomain);
        Language agentInputLanguage = (detectedLanguage == Language.AMHARIC)
                ? Language.ENGLISH : detectedLanguage;
        String agentResponse = domainAgent.processUserQuestion(
                questionForProcessing, resolvedSessionId, agentInputLanguage);
        final String finalResponse;
        if (detectedLanguage == Language.AMHARIC) {
            finalResponse = translationOrchestrationService
                    .translateFromEnglish(agentResponse, Language.AMHARIC);
        } else {
            finalResponse = agentResponse;
        }
        logger.info("[Session {}] Completed in {} ms", resolvedSessionId,
                System.currentTimeMillis() - startTime);
        // Delegate to a separate bean so @Async is honoured by Spring's proxy
        asyncPersistenceService.persistInteractionAsync(new UserInteractionDto(
                resolvedSessionId,
                chatRequest.getUserInput(),
                detectedLanguage,
                translatedToEnglish.equals(chatRequest.getUserInput()) ? null : translatedToEnglish,
                detectedDomain,
                agentResponse,
                finalResponse));
        userSessionManager.recordSessionTurn(resolvedSessionId, chatRequest.getUserInput(),
                finalResponse, detectedLanguage, detectedDomain);
        return ChatResponse.builder()
                .responseText(finalResponse)
                .detectedLanguage(detectedLanguage.name())
                .detectedDomain(detectedDomain.name())
                .sessionId(resolvedSessionId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}