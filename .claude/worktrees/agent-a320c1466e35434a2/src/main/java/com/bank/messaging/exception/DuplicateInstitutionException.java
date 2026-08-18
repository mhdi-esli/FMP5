package com.bank.messaging.exception;

/**
 * Thrown when attempting to create an institution with a duplicate institutionId.
 */
public class DuplicateInstitutionException extends RuntimeException {

    public DuplicateInstitutionException(String institutionId) {
        super("Institution already exists: " + institutionId);
    }
}
