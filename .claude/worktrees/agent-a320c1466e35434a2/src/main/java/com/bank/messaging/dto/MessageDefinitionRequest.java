package com.bank.messaging.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating a message definition mapping.
 * Field mappings and validation rules are stored as raw JSON strings (TQ-1).
 */
public record MessageDefinitionRequest(
    @NotBlank(message = "Message type is required")
    String messageType,

    @NotBlank(message = "Network is required")
    String network,

    @NotNull(message = "Version is required")
    @Min(value = 1, message = "Version must be positive")
    Integer version,

    String fieldMappings,

    String validationRules,

    Boolean isActive
) {}
