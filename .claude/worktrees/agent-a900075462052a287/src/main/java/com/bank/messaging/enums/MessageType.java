package com.bank.messaging.enums;

import lombok.Getter;

/**
 * Supported message types for financial institution transfers.
 */
@Getter
public enum MessageType {
    MT200("200", "Financial Institution Transfer");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
