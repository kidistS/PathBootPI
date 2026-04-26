package com.pathboot.exception;

import com.pathboot.model.response.ChatResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for all REST controllers.
 *
 * <p>Converts application exceptions into structured {@link ChatResponse} error payloads
 * so the client always receives a consistent JSON response shape.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    // ─── Domain not found ────────────────────────────────────────────────────
    @ExceptionHandler(DomainNotFoundException.class)
    public ResponseEntity<ChatResponse> handleDomainNotFoundException(DomainNotFoundException ex) {
        logger.error("Domain not found: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ─── Translation failure ─────────────────────────────────────────────────
    @ExceptionHandler(TranslationException.class)
    public ResponseEntity<ChatResponse> handleTranslationException(TranslationException ex) {
        logger.error("Translation error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Translation service encountered an error: " + ex.getMessage());
    }

    // ─── LLM communication failure ───────────────────────────────────────────
    @ExceptionHandler(LlmCommunicationException.class)
    public ResponseEntity<ChatResponse> handleLlmCommunicationException(LlmCommunicationException ex) {
        logger.error("LLM communication error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "AI model service is currently unavailable. Please try again later.");
    }

    // ─── Bean validation failure ─────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ChatResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessages = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("Validation failed: {}", errorMessages);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + errorMessages);
    }

    // ─── Catch-all ───────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.");
    }

    // ─── Helper ──────────────────────────────────────────────────────────────
    private ResponseEntity<ChatResponse> buildErrorResponse(HttpStatus status, String message) {
        ChatResponse errorResponse = ChatResponse.builder()
                .responseText("ERROR: " + message)
                .detectedLanguage("UNKNOWN")
                .detectedDomain("UNKNOWN")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }
}

