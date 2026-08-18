package com.bank.messaging.exception;

/**
 * Thrown when a JSON string field contains invalid JSON syntax.
 */
public class InvalidJsonException extends RuntimeException {

    public InvalidJsonException(String fieldName) {
        super("Invalid JSON format for field: " + fieldName);
    }
}
