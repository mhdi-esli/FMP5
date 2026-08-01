package com.bank.messaging.enums;

/**
 * Supported messaging networks for financial transfers.
 */
public enum Network {
    SWIFT,
    SEPA;

    public static Network fromCode(String code) {
        for (Network network : values()) {
            if (network.name().equalsIgnoreCase(code)) {
                return network;
            }
        }
        return null;
    }
}
