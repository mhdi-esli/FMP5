package com.bank.messaging.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InstitutionRequest validation.
 */
class InstitutionRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequest_shouldPassValidation() {
        InstitutionRequest request = new InstitutionRequest(
                "BANK01", "BANK01XXX", "Test Bank",
                "BR001", true, List.of("SWIFT", "SEPA")
        );

        Set<ConstraintViolation<InstitutionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void missingInstitutionId_shouldFailValidation() {
        InstitutionRequest request = new InstitutionRequest(
                null, "BANK01XXX", "Test Bank",
                null, true, null
        );

        Set<ConstraintViolation<InstitutionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("institutionId")));
    }

    @Test
    void missingName_shouldFailValidation() {
        InstitutionRequest request = new InstitutionRequest(
                "BANK01", "BANK01XXX", null,
                null, true, null
        );

        Set<ConstraintViolation<InstitutionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void emptyNetworksList_shouldBeAllowed() {
        InstitutionRequest request = new InstitutionRequest(
                "BANK01", "BANK01XXX", "Test Bank",
                null, true, List.of()
        );

        Set<ConstraintViolation<InstitutionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Empty networks list should be valid");
    }
}
