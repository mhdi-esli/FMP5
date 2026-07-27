package com.bank.messaging.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ErrorCode enum values and their Persian messages.
 */
class ErrorCodeTest {

    @Test
    void msg008_codeAndMessages_shouldExist() {
        ErrorCode code = ErrorCode.MSG_008;

        assertEquals("MSG-008", code.getCode());
        assertEquals("تعریف پیام تکراری است", code.getPersianMessage());
        assertEquals("Duplicate message definition", code.getEnglishDescription());
    }

    @Test
    void msg009_codeAndMessages_shouldExist() {
        ErrorCode code = ErrorCode.MSG_009;

        assertEquals("MSG-009", code.getCode());
        assertEquals("تعریف پیام یافت نشد", code.getPersianMessage());
        assertEquals("Message definition not found", code.getEnglishDescription());
    }

    @Test
    void msg010_codeAndMessages_shouldExist() {
        ErrorCode code = ErrorCode.MSG_010;

        assertEquals("MSG-010", code.getCode());
        assertEquals("مؤسسه فرستنده یافت نشد", code.getPersianMessage());
        assertEquals("Sender institution not found", code.getEnglishDescription());
    }

    @Test
    void msg011_codeAndMessages_shouldExist() {
        ErrorCode code = ErrorCode.MSG_011;

        assertEquals("MSG-011", code.getCode());
        assertEquals("مؤسسه فرستنده فعال نیست", code.getPersianMessage());
        assertEquals("Sender institution is inactive", code.getEnglishDescription());
    }

    @Test
    void msg012_codeAndMessages_shouldExist() {
        ErrorCode code = ErrorCode.MSG_012;

        assertEquals("MSG-012", code.getCode());
        assertEquals("مؤسسه از شبکه انتخاب‌شده پشتیبانی نمی‌کند", code.getPersianMessage());
        assertEquals("Institution does not support the selected network", code.getEnglishDescription());
    }

    @Test
    void allErrorCodes_shouldHaveUniqueCodes() {
        ErrorCode[] values = ErrorCode.values();
        long uniqueCodes = java.util.Arrays.stream(values)
                .map(ErrorCode::getCode)
                .distinct()
                .count();
        assertEquals(values.length, uniqueCodes, "Each ErrorCode must have a unique code");
    }

    @Test
    void allErrorCodes_shouldHaveNonEmptyMessages() {
        for (ErrorCode code : ErrorCode.values()) {
            assertNotNull(code.getCode(), "Code should not be null for " + code.name());
            assertNotNull(code.getPersianMessage(), "Persian message should not be null for " + code.name());
            assertNotNull(code.getEnglishDescription(), "English description should not be null for " + code.name());
            assertFalse(code.getCode().isEmpty(), "Code should not be empty for " + code.name());
            assertFalse(code.getPersianMessage().isEmpty(), "Persian message should not be empty for " + code.name());
            assertFalse(code.getEnglishDescription().isEmpty(), "English description should not be empty for " + code.name());
        }
    }
}
