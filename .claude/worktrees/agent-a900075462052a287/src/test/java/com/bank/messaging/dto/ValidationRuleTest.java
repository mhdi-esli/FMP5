package com.bank.messaging.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationRule record structure.
 */
class ValidationRuleTest {

    @Test
    void shouldCreateValidationRuleWithAllFields() {
        ValidationRule rule = new ValidationRule("amount_positive", true, "{\"min\": \"0.01\"}");

        assertEquals("amount_positive", rule.ruleName());
        assertTrue(rule.enabled());
        assertEquals("{\"min\": \"0.01\"}", rule.params());
    }

    @Test
    void shouldCreateDisabledRuleWithNullParams() {
        ValidationRule rule = new ValidationRule("currency_iso", false, null);

        assertEquals("currency_iso", rule.ruleName());
        assertFalse(rule.enabled());
        assertNull(rule.params());
    }
}
