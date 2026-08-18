package com.bank.messaging.enums;

import lombok.Getter;

/**
 * Error codes for financial messaging platform.
 * Messages are in Persian as per requirements.
 */
@Getter
public enum ErrorCode {
    MSG_000("MSG-000", "پیام با موفقیت ایجاد شد", "Message created successfully"),
    MSG_001("MSG-001", "اطلاعات ورودی معتبر نیست", "Invalid input data"),
    MSG_002("MSG-002", "نوع پیام معتبر نیست", "Invalid message type"),
    MSG_003("MSG-003", "شبکه انتخاب‌شده پشتیبانی نمی‌شود", "Network not supported"),
    MSG_004("MSG-004", "Message Definition یافت نشد", "Message Definition not found"),
    MSG_005("MSG-005", "Message Definition غیرفعال است", "Message Definition inactive"),
    MSG_006("MSG-006", "اعتبارسنجی پیام ناموفق بود", "Message validation failed"),
    MSG_007("MSG-007", "ایجاد پیام با خطا مواجه شد", "Message creation error"),
    MSG_008("MSG-008", "تعریف پیام تکراری است", "Duplicate message definition"),
    MSG_009("MSG-009", "تعریف پیام یافت نشد", "Message definition not found"),
    MSG_010("MSG-010", "مؤسسه فرستنده یافت نشد", "Sender institution not found"),
    MSG_011("MSG-011", "مؤسسه فرستنده فعال نیست", "Sender institution is inactive"),
    MSG_012("MSG-012", "مؤسسه از شبکه انتخاب‌شده پشتیبانی نمی‌کند", "Institution does not support the selected network");

    private final String code;
    private final String persianMessage;
    private final String englishDescription;

    ErrorCode(String code, String persianMessage, String englishDescription) {
        this.code = code;
        this.persianMessage = persianMessage;
        this.englishDescription = englishDescription;
    }
}
