package com.bank.messaging.service;

import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.enums.ValidationResultEnum;

import java.util.List;

/**
 * Result of message validation.
 */
public record ValidationResult(
    ValidationResultEnum result,
    List<ValidationError> errors
) {
    public static ValidationResult success() {
        return new ValidationResult(ValidationResultEnum.SUCCESS, List.of());
    }

    public static ValidationResult failed(List<ValidationError> errors) {
        return new ValidationResult(ValidationResultEnum.FAILED, errors);
    }

    public static ValidationResult failed(ValidationError error) {
        return failed(List.of(error));
    }

    public boolean isSuccess() {
        return result == ValidationResultEnum.SUCCESS;
    }
}
