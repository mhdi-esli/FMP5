package com.bank.messaging.enums;

import lombok.Getter;

/**
 * Result of message validation.
 */
@Getter
public enum ValidationResultEnum {
    SUCCESS("Success", "Validation passed"),
    FAILED("Failed", "Validation failed");

    private final String displayName;
    private final String description;

    ValidationResultEnum(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
