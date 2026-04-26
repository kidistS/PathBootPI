package com.pathboot.exception;

/**
 * Thrown when no agent or service can be found for a requested domain type.
 */
public class DomainNotFoundException extends RuntimeException {

    public DomainNotFoundException(String message) {
        super(message);
    }

    public DomainNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

