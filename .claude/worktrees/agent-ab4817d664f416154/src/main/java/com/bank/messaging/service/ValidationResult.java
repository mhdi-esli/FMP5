package com.bank.messaging.service;

import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.enums.ValidationResultEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of message validation.
 */
public record ValidationResult(
    ValidationResultEnum result,
    List<ValidationError> errors
) {
    public static ValidationResult success() {
        return new ValidationResult(ValidationResultEnum.SUCCESS, Collections.emptyList());
    }

    public static ValidationResult failed(List<ValidationError> errors) {
        return new ValidationResult(ValidationResultEnum.FAILED, errors);
    }

    public static ValidationResult failed(ValidationError error) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(ValidationResultEnum.FAILED, errors);
    }

    public boolean isSuccess() {
        return ValidationResultEnum.SUCCESS.equals(result);
    }
}
