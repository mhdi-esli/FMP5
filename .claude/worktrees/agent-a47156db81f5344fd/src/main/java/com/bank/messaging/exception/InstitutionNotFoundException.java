package com.bank.messaging.exception;

/**
 * Thrown when an institution is not found by its business ID.
 */
public class InstitutionNotFoundException extends RuntimeException {

    public InstitutionNotFoundException(String institutionId) {
        super("Institution not found: " + institutionId);
    }
}
