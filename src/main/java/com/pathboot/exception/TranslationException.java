package com.pathboot.exception;

/**
 * Thrown when a translation operation fails (either via Ollama or the NLLB server).
 */
public class TranslationException extends RuntimeException {

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}

