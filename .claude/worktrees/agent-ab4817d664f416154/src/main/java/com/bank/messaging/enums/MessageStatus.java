package com.bank.messaging.enums;

import lombok.Getter;

/**
 * Status of a financial message in its lifecycle.
 */
@Getter
public enum MessageStatus {
    DRAFT("Draft", "Message created, not yet sent"),
    VALIDATION_FAILED("Validation Failed", "Message failed validation");

    private final String displayName;
    private final String description;

    MessageStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
