package com.bank.messaging.exception;

/**
 * Thrown when attempting to create a duplicate message definition
 * (same messageType + network + version combination).
 */
public class DuplicateDefinitionException extends RuntimeException {

    public DuplicateDefinitionException(String messageType, String network, Integer version) {
        super("Message definition already exists for " + messageType + "/" + network + " v" + version);
    }
}
