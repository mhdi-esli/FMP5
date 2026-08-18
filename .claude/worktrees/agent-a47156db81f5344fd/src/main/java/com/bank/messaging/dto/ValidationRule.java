package com.bank.messaging.dto;

/**
 * Value object representing a validation rule for a message definition.
 * Used within the validation_rules JSONB structure.
 * Stored as raw JSON string per TQ-1; this record is for consumer convenience.
 */
public record ValidationRule(
    String ruleName,
    boolean enabled,
    String params
) {}
