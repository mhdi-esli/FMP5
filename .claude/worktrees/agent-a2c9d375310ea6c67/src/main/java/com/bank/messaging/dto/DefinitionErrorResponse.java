package com.bank.messaging.dto;

/**
 * Simple error response for definition management errors.
 */
public record DefinitionErrorResponse(
    String code,
    String message
) {}
