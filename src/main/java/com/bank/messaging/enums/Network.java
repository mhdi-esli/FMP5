package com.bank.messaging.enums;

import lombok.Getter;

/**
 * Supported messaging networks for financial transfers.
 */
@Getter
public enum Network {
    SWIFT("SWIFT", "SWIFT Network"),
    SEPA("SEPA", "SEPA Network");

    private final String code;
    private final String description;

    Network(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Network fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (Network network : values()) {
            if (network.code.equalsIgnoreCase(code)) {
                return network;
            }
        }
        return null;
    }
}
