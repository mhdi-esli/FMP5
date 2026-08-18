package com.bank.messaging.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageDefinitionRequest validation.
 */
class MessageDefinitionRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequest_shouldPassValidation() {
        MessageDefinitionRequest request = new MessageDefinitionRequest(
                "MT200", "SWIFT", 1,
                "{\"field1\": \"value1\"}",
                "{\"rule1\": \"active\"}",
                true
        );

        Set<ConstraintViolation<MessageDefinitionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void missingMessageType_shouldFailValidation() {
        MessageDefinitionRequest request = new MessageDefinitionRequest(
                null, "SWIFT", 1,
                null, null, true
        );

        Set<ConstraintViolation<MessageDefinitionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("messageType")));
    }

    @Test
    void missingNetwork_shouldFailValidation() {
        MessageDefinitionRequest request = new MessageDefinitionRequest(
                "MT200", null, 1,
                null, null, true
        );

        Set<ConstraintViolation<MessageDefinitionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("network")));
    }

    @Test
    void nullVersion_shouldFailValidation() {
        MessageDefinitionRequest request = new MessageDefinitionRequest(
                "MT200", "SWIFT", null,
                null, null, true
        );

        Set<ConstraintViolation<MessageDefinitionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("version")));
    }

    @Test
    void negativeVersion_shouldFailValidation() {
        MessageDefinitionRequest request = new MessageDefinitionRequest(
                "MT200", "SWIFT", -1,
                null, null, true
        );

        Set<ConstraintViolation<MessageDefinitionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("version")));
    }
}
