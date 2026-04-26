package com.pathboot.exception;

/**
 * Thrown when communication with an LLM backend (Ollama or NLLB server) fails.
 */
public class LlmCommunicationException extends RuntimeException {

    public LlmCommunicationException(String message) {
        super(message);
    }

    public LlmCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

