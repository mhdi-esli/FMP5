package com.bank.messaging.dto;

/**
 * Represents a single validation error.
 */
public record ValidationError(
    String code,
    String field,
    String message
) {}
