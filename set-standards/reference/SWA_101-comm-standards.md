# Inter-Service Communication Standards (SWA_101 v1.2)
# Source: Dotin Software Architecture Standard — enforced for ALL code generation

> These standards MUST be followed in all generated code involving REST APIs,
> message brokers, or any inter-service communication. No exceptions without
> explicit instruction.

---

## 1. REST API — URL Structure

Pattern: `/v{major}/{resource-collection}/{resource-id}/{sub-resource}`

### Rules
- **NEVER** add an `/api/` prefix to URLs. Use a subdomain instead (e.g., `api.dotin.ir`).
- **ALWAYS** use `kebab-case` for ALL path segments (e.g., `customer-management`, NOT `customerManagement`).
- **ALWAYS** version with only the major version: `/v1/`, `/v2/`. Never `/v1.2/`.
- **ALWAYS** use plural nouns for collections (`/accounts`), singular only when truly one resource (`/signature`).
- **NEVER** use verbs in paths. Actions are expressed by HTTP methods.
  - ✅ `POST /v1/transactions`
  - ❌ `POST /v1/create-transaction`
  - Exception (RPC-style, only when no RESTful alternative exists): `PUT /v1/loans/{id}/approve`
- Nest sub-resources under their parent. Max **3 levels** of nesting.
  - ✅ `/v1/customers/{customerId}/accounts/{accountId}/transactions/{txId}`
  - ❌ `/v1/addresses/{addressId}` (if address is always owned by a customer)
- Use `self` as a pseudo-ID when the resource identity comes from the auth token:
  - `/v1/employees/self`, `/v1/employees/self/personal-details`
- **Query parameters**: `camelCase` only.
  - ✅ `?branchCode=410`  ❌ `?branch-code=410`
- **Pagination**:
  - Offset-based: `?page=2&size=10&sortBy=date&order=desc`
  - Cursor-based (preferred): `?cursor=ewr4s8&size=10&sortBy=date&order=desc`

---

## 2. REST API — Request Format

### Required Headers (ALL outgoing requests)
```
Idempotency-Key:      <UUID>                         # e.g. 123e4567-e89b-42d3-a456-556642440000
X-Request-DateTime:   <UTC ISO 8601, no offset>      # e.g. 2025-08-22T14:30:00.123Z
Accept-Language:      <fa | en-US | ...>
Authorization:        Bearer <access_token>
traceparent:          <W3C Trace Context>             # e.g. 00-<traceId>-<parentId>-01
```

- `Idempotency-Key` stays **the same** across all retries of ONE logical operation.
- Each independent logical operation gets its **own** unique `Idempotency-Key`.
- `X-Request-DateTime` is always UTC, ISO 8601, **no timezone offset**.

### Request Body
```json
{
  "metadata": {           // OPTIONAL — non-business, processing hints only
    "tags": ["deposit"],
    "extraInfo": {}
  },
  // Business fields go DIRECTLY here — NOT nested under a "payload" key
  "firstName": "علی",
  "nationalCode": "1234567890"
}
```
- **NEVER** wrap business fields under a `payload` key.
- `metadata` is optional; document it fully if used.

---

## 3. REST API — Response Format

### Required Response Headers
```
Idempotency-Key:          <echo from request>
X-Request-DateTime:       <echo from request>
X-Idempotency-Replayed:   true | false      # true = served from cache
X-Response-DateTime:      <UTC ISO 8601>
traceparent:              <updated W3C Trace Context>
```

### Response Body
```json
{
  "RsCode":    "0",       // LEGACY ONLY — include for backward compat, remove on schedule
  "IsSuccess": "true",    // LEGACY ONLY — include for backward compat, remove on schedule
  "metadata":   null,     // optional
  "resultData": {         // business payload here
    "transactionId": "TXN_987654321",
    "status": "COMPLETED"
  },
  "message": "عملیات با موفقیت انجام شد",   // human-readable summary
  "errorList": []         // ALWAYS present; empty array [] on success — NEVER null
}
```

### Error Detection Rule
- **ALWAYS** check `errorList`. If it has one or more items → operation failed.
- `errorList` with items **MUST** accompany a 4xx or 5xx HTTP status. Never return errors with 2xx.
- On success: `"errorList": []` — not `null`, not omitted.

### Error Object Structure
```json
{
  "issuer":      "PAY",                    // 2–6 uppercase chars, unique per product/service
  "code":        105,                      // integer, unique per issuer; 1–200 are RESERVED
  "description": "Human-readable message",
  "details": [                             // optional
    { "path": "/v1/customers/10025/#phoneNumber" }  // format: /path/#fieldName
  ]
}
```

### Reserved Error Codes (codes 1–200 are global — NEVER reuse for custom errors)
| Code | HTTP Status | Meaning |
|------|-------------|---------|
| 1–100 | varies | Reserved for Shetab (شتابی) codes |
| 101  | 400 | Invalid username or password |
| 102  | 401 | Auth token missing or invalid |
| 103  | 403 | Current user lacks permission |
| 104  | 425 | Duplicate in-progress request (race condition) |
| 105  | 409 | Duplicate request processed by a different call |
| 106  | 400 | Idempotency-Key required but not provided |
| 107  | 429 | Rate limit exceeded |
| 108–200 | — | Reserved |

Custom service error codes start from **201** onwards.

---

## 4. HTTP Status Codes — Allowed Set

| Code | Use when |
|------|----------|
| 200  | Successful GET, PUT, PATCH |
| 201  | Successful POST (resource created) |
| 202  | Accepted for async processing |
| 204  | Successful DELETE (or mutation with no body) |
| 400  | Client error — bad request, invalid input, business validation failure |
| 401  | Missing or invalid auth token |
| 403  | Authenticated but unauthorized |
| 404  | Resource not found |
| 405  | Method not allowed or operation not supported on current state |
| 409  | Conflict — resource already exists or state conflict |
| 422  | Valid format but business rules violated |
| 425  | Too Early — same request already in-flight |
| 429  | Rate limit exceeded |
| 500  | Unexpected server error |
| 502  | Downstream service unavailable |
| 503  | Service temporarily unavailable |

**Do NOT use 422 for general input validation** — use 400 instead.

---

## 5. Paginated Responses

Pagination metadata goes inside `metadata.pagination`, NOT in `resultData`:
```json
{
  "metadata": {
    "pagination": {
      "nextCursor": "ejp1w2",
      "previousCursor": "nud3a8"
    }
  },
  "resultData": { ... },
  "errorList": []
}
```

---

## 6. Data Formatting Rules

1. **Format**: JSON only for all request/response bodies.
2. **Field names**: `camelCase` always.
3. **Dates**: ISO 8601 UTC, no offset — `"2025-08-22T14:30:00.123Z"`. Never `+03:30` in payload.
4. **Numbers**: Use `integer` or `string` for large IDs. **Never `float` for monetary values.**
5. **Enums**: String-based (`"ACTIVE"`, `"PENDING"`) — never numeric enum values.
6. **Array field names**: Plural (`"phones": []`, `"accounts": []`).
7. **Empty arrays**: Always `[]`, never `null`.
8. **Boolean fields**: Never `null`. If three states are needed, use a string enum: `"YES" | "NO" | "UNDEFINED"`.
9. **Null vs absent**:
   - `optional + nullable` → prefer `{"field": null}` over omitting.
   - `required + non-nullable` → must always appear with a non-null value.

---

## 7. Async Messaging (Message Brokers)

Same Header/Body contract as REST. The structure is intentionally identical to minimize protocol-switching cost.

### Message Headers
```
Idempotency-Key:     <UUID>
X-Request-DateTime:  <UTC ISO 8601>
Accept-Language:     <fa | en-US>
Authorization:       Bearer <access_token>
```

### Response Message Additional Header
```
X-Idempotency-Replayed:  true | false
X-Response-DateTime:     <UTC ISO 8601>
X-Response-Code:         <HTTP equivalent — e.g. 200, 400, 404>
```

### Message Body (Request)
```json
{
  "metadata": { ... },  // optional
  // business fields directly here
}
```

### Message Body (Response)
```json
{
  "RsCode":     "...",  // legacy
  "IsSuccess":  "...",  // legacy
  "metadata":   { ... },
  "resultData": { ... },
  "message":    "...",
  "errorList":  []
}
```

### Brokers Without Native Header Support
Embed headers inside the body under a `header` key:
```json
{
  "header": {
    "Idempotency-Key":        "...",
    "X-Request-DateTime":     "...",
    "Accept-Language":        "...",
    "Authorization":          "Bearer ..."
  },
  "metadata": { ... },
  // business fields
}
```

### Channel Naming Convention
```
[prefix].[system].[domain].{component}.[event|command[.request|.response]].[topic|queue].v[N]{.dlt}
```
- `prefix`: always `corridor`
- `system`: e.g. `core`, `esb`, `bank-mobile`, `switch`
- `domain`: service/domain name, e.g. `deposit`
- `component` (optional): sub-component, e.g. `ach`
- `event` (past tense, kebab-case): e.g. `status-changed`
- `command` (imperative, kebab-case): e.g. `close-deposit`
- `request|response` — only for command channels
- `topic|queue`: based on Pub/Sub vs Point-to-Point
- `v[N]`: integer version, e.g. `v1`
- `.dlt`: suffix for Dead Letter queues

**Examples:**
```
corridor.core.deposit.status-changed.topic.v1
corridor.core.deposit.close-deposit.request.queue.v1
corridor.core.deposit.close-deposit.response.topic.v1
corridor.core.deposit.close-deposit.request.queue.v1.dlt
```

---

## 8. Idempotency Implementation

- `Idempotency-Key` is a UUID, generated by the **client** per logical operation.
- Same key used for **all retries** of the same logical operation.
- Each **independent** logical operation gets its own key.
- Server MUST store the key + response result and replay cached response on duplicate.
- Process `Idempotency-Key` ONLY AFTER Authentication and Authorization.
- Recommended: also store a **Fingerprint** (checksum of body or selected fields) alongside the key to detect mismatched payloads.

### Idempotency Error Handling
| Situation | HTTP | Error Code |
|-----------|------|------------|
| Same key, same payload, request still in-flight | 425 | 104 |
| Same key, different payload (fingerprint mismatch) | 409 | 105 |
| Key required by API but not provided | 400 | 106 |

### `Idempotency-Key` vs `X-Request-Id`
- `Idempotency-Key` → represents the **logical** operation (same across retries).
- `X-Request-Id` (optional) → represents the **physical** HTTP request (unique per network call).

---

## 9. Security

- All inter-service calls MUST be authenticated.
- Use **OAuth 2.0 Client Credentials** flow via Dotin SSO.
- Tokens MUST be **JWS** type (revocable, expirable). Max TTL: **4 hours**.
- **Never** hardcode or permanently store the SSO public key. Use cache + evict-on-error.
- Token goes in `Authorization: Bearer <token>` header (or `metadata.authorization` for gRPC intra-domain).
- Validate: JWS signature, expiry, required scopes. Check revocation via SSO event bus topic.
- The **request payload MUST be signed** by the sender. Server verifies signature before processing.
- Authorization is managed by AMS (Access Management Service). Declare required permissions in API docs.

---

## 10. Distributed Tracing (OpenTelemetry)

- ALL services MUST implement the **OpenTelemetry SDK**.
- Use **W3C Trace Context (W3C)** standard for propagation — `traceparent` header is **mandatory**.
- `traceparent` format: `00-{traceId}-{parentId}-{traceFlags}`
- `tracestate` is optional (for APM tools).
- All structured (JSON) logs MUST include current `trace_id` and `span_id`.
- **NEVER log full request/response body** in any environment (PII risk).
- If partial logging is needed (e.g. on 5xx), sanitize first — mask sensitive fields: `pan`, `cvv2`, `password`, `token`, `nationalCode`, etc. → replace with `***`.
- Log metadata only: `http.method`, `http.path`, `http.status_code`, `duration_ms`, non-sensitive business IDs.

---

## 11. Protocol Selection

| Scenario | Protocol |
|----------|----------|
| Cross-domain / external APIs | **REST** (mandatory) |
| Intra-domain / high-performance internal | gRPC (allowed) |
| Legacy banking partner integration | SOAP (only if unavoidable) |
| **SOAP within Dotin ecosystem** | ❌ **Never allowed** |

---

## Quick Reference — Standard Error Response Example

```json
// POST /v1/login   →   HTTP 400
{
  "errorList": [
    {
      "issuer": "USR",
      "code": 101,
      "description": "نام کاربری یا رمز عبور نامعتبر است"
    }
  ],
  "resultData": null,
  "message": "درخواست نامعتبر است",
  "errorList": []
}
```

```
HTTP/1.1 400 Bad Request
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
X-Idempotency-Replayed: false
X-Response-DateTime: 2025-08-22T14:30:02.000Z
```

---

*Based on SWA_101 Inter-Service Communication Standards v1.2 — Dotin Platform*
