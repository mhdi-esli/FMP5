package com.bank.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating or updating a financial institution.
 */
public record InstitutionRequest(
    @NotBlank(message = "Institution ID is required")
    String institutionId,

    @Size(min = 8, max = 11, message = "BIC must be between 8 and 11 characters")
    String bic,

    @NotBlank(message = "Institution name is required")
    String name,

    String branchId,

    Boolean isActive,

    List<String> supportedNetworks
) {}
