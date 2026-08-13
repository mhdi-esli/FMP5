# API and Messaging Standards

## URL Structure

**Rule:** All API endpoints must follow the pattern `/v{major}/{resource-collection}`

- **NEVER** use `/api/` prefix
- **ALWAYS** use kebab-case for resource names
- **ALWAYS** version with major version only (`/v1/`, `/v2/`)

**Examples:**
- ✅ `POST /v1/messages`
- ✅ `GET /v1/message-definitions`
- ❌ `POST /api/v1/messages`
- ❌ `POST /v1/create-message`

**Verification Method:** Parse controller `@RequestMapping` annotations

---

## HTTP Status Codes

**Rule:** Use only the allowed status codes set

| Code | Use Case |
|------|----------|
| 200 | Successful GET, PUT, PATCH |
| 201 | Successful POST (resource created) |
| 204 | Successful DELETE |
| 400 | Client error, validation failure, business rules |
| 401 | Missing/invalid auth token |
| 403 | Unauthorized |
| 404 | Resource not found |
| 409 | Conflict (duplicate resource) |
| 422 | Valid format but business rules violated |
| 500 | Unexpected server error |

**Never use 422 for general input validation** — use 400 instead.

**Verification Method:** Check controller return status codes

---

## Error Codes

**Rule:** Error codes must use `{DOMAIN}-{NUMBER}` format with Persian messages

- **Format:** `XXX-NNN` (e.g., `MSG-001`, `MSG-002`)
- **Language:** Persian messages for all errors
- **Error Codes for this project:**
  - MSG-000: پیام با موفقیت ایجاد شد
  - MSG-001: اطلاعات ورودی معتبر نیست
  - MSG-002: نوع پیام معتبر نیست
  - MSG-003: شبکه انتخاب‌شده پشتیبانی نمی‌شود
  - MSG-004: Message Definition یافت نشد
  - MSG-005: Message Definition غیرفعال است
  - MSG-006: اعتبارسنجی پیام ناموفق بود
  - MSG-007: ایجاد پیام با خطا مواجه شد
  - MSG-008: تعریف پیام تکراری است (Duplicate message definition)
  - MSG-009: تعریف پیام یافت نشد (Message definition not found)
  - MSG-010: مؤسسه فرستنده نامعتبر است
  - MSG-011: مؤسسه گیرنده نامعتبر است
  - MSG-012: تطبیق مؤسسه با شبکه ناموفق است

**Verification Method:** Scan ErrorCode enum and error handlers

---

## Response Envelope Format

**Rule:** All responses must follow this structure

```json
{
  "resultData": { ... } | null,
  "message": "Human-readable summary in Persian",
  "errorList": [ ... ]
}
```

- `resultData`: Business payload (or null on error)
- `message`: Human-readable Persian summary
- `errorList`: Always present, empty array `[]` on success

**Verification Method:** Check response DTOs

---

## Validation

**Rule:** Validate all inputs with proper error messages

- Use Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Pattern`)
- Collect all validation errors and return together
- Persian messages for all validation errors

---

## OpenAPI Documentation

**Rule:** All endpoints must be documented in OpenAPI spec

- Use SpringDoc annotations (`@Operation`, `@ApiResponse`)
- Export YAML to `/documents/openapi/openapi.yaml`
