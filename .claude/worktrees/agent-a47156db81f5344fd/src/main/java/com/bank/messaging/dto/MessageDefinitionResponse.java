package com.bank.messaging.dto;

import com.bank.messaging.entity.MessageDefinitionMapping;

import java.time.LocalDateTime;

/**
 * Response DTO for message definition operations.
 */
public record MessageDefinitionResponse(
    Long id,
    String messageType,
    String network,
    Integer version,
    String fieldMappings,
    String validationRules,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static MessageDefinitionResponse fromEntity(MessageDefinitionMapping entity) {
        return new MessageDefinitionResponse(
                entity.getId(),
                entity.getMessageType(),
                entity.getNetwork(),
                entity.getVersion(),
                entity.getFieldMappings(),
                entity.getValidationRules(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
