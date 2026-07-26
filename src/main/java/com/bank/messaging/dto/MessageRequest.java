package com.bank.messaging.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating a financial institution transfer message.
 */
public record MessageRequest(
    @NotBlank(message = "Message type is required")
    String messageType,

    @NotBlank(message = "Network is required")
    String network,

    @NotBlank(message = "Request reference is required")
    String requestReference,

    @NotBlank(message = "Transaction reference is required")
    String transactionReference,

    String relatedReference,

    @NotBlank(message = "Value date is required")
    String valueDate,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    String currency,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "Sender institution identifier is required")
    String senderInstitutionIdentifier,

    String senderBIC,
    String senderBranchIdentifier,

    @NotBlank(message = "Receiver institution identifier is required")
    String receiverInstitutionIdentifier,

    String receiverBIC,
    String receiverBranchIdentifier,

    String chargeType,
    String instructionCode,
    String narrative,
    String additionalInformation
) {}
