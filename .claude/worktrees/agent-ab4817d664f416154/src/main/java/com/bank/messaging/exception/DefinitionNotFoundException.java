package com.bank.messaging.exception;

/**
 * Thrown when a message definition is not found by ID.
 */
public class DefinitionNotFoundException extends RuntimeException {

    public DefinitionNotFoundException(Long id) {
        super("Message definition not found with id: " + id);
    }
}
