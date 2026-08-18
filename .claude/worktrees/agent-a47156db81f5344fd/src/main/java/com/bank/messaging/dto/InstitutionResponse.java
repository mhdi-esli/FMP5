package com.bank.messaging.dto;

import com.bank.messaging.entity.Institution;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for institution operations.
 */
public record InstitutionResponse(
    Long id,
    String institutionId,
    String bic,
    String name,
    String branchId,
    Boolean isActive,
    List<String> supportedNetworks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static InstitutionResponse fromEntity(Institution entity) {
        return new InstitutionResponse(
                entity.getId(),
                entity.getInstitutionId(),
                entity.getBic(),
                entity.getName(),
                entity.getBranchId(),
                entity.getIsActive(),
                entity.getSupportedNetworks(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
