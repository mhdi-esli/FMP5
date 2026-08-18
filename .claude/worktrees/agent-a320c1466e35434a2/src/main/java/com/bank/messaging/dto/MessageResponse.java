package com.bank.messaging.dto;

import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.enums.ValidationResultEnum;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for message creation.
 */
public record MessageResponse(
    String messageId,
    String messageType,
    String network,
    MessageStatus status,
    LocalDateTime creationDateTime,
    ValidationResultEnum validationResult,
    List<ValidationError> validationErrors
) {}
