package com.bank.messaging.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EPIC-03 custom exceptions.
 */
class InstitutionExceptionTest {

    @Test
    void institutionNotFoundException_shouldHaveCorrectMessage() {
        InstitutionNotFoundException ex = new InstitutionNotFoundException("BANK99");

        assertEquals("Institution not found: BANK99", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void duplicateInstitutionException_shouldHaveCorrectMessage() {
        DuplicateInstitutionException ex = new DuplicateInstitutionException("BANK01");

        assertEquals("Institution already exists: BANK01", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
