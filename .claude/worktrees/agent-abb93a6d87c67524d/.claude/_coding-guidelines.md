# Coding Guidelines

**Last Updated:** 2026-07-26
**Project:** Financial Messaging Platform

This document is the authoritative source for coding standards and conventions. All advisors and skills must consult this before making code quality recommendations.

---

## Language & Framework

| Aspect | Decision |
|--------|----------|
| Language | Java 21+ |
| Framework | Spring Boot 4.x |
| Build Tool | Maven |
| Encoding | UTF-8 everywhere |

---

## Project Structure

```
com.bank.messaging/
├── controller/       # REST endpoints
├── service/          # Business logic
├── repository/       # Data access (Spring Data JPA)
├── entity/           # JPA entities
├── dto/              # Request/Response DTOs
├── enums/            # Enums (MessageType, Network, Status, etc.)
├── records/          # Java records (MessageDefinition, ValidationRule)
├── config/           # Spring @Configuration classes
├── exception/        # Custom exceptions, @ControllerAdvice
├── validator/        # Custom validation logic
└── util/             # Helper utilities
```

---

## Naming Conventions

### Classes

| Type | Convention | Example |
|------|------------|---------|
| Controller | `{Entity}Controller` | `MessageController` |
| Service | `{Entity}{Action}Service` | `MessageCreationService` |
| Repository | `{Entity}Repository` | `MessageRepository` |
| Entity | `{Entity}` (singular) | `Message`, `Institution` |
| DTO (Request) | `{Entity}Request` | `MessageRequest` |
| DTO (Response) | `{Entity}Response` | `MessageResponse` |
| Exception | `{Entity}{Reason}Exception` | `MessageValidationException` |
| Validator | `{Entity}Validator` | `MessageRequestValidator` |

### Methods

| Type | Convention | Example |
|------|------------|---------|
| Service (create) | `create{Entity}` | `createMessage()` |
| Service (find) | `find{Entity}By{Criteria}` | `findMessageById()` |
| Service (validate) | `validate{Entity}` | `validateMessage()` |
| Repository | Spring Data conventions | `findById()`, `save()` |
| Controller | REST verb | `createMessage()` for POST |

### Variables

| Type | Convention | Example |
|------|------------|---------|
| Local | camelCase | `messageId`, `validationResult` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Fields | camelCase, private | `private String messageId;` |

---

## Java Records

Use Java records for immutable data carriers:

```java
// DTOs
public record MessageRequest(
    String messageType,
    String network,
    String requestReference,
    // ...
) {}

// Domain value objects
public record ValidationError(
    String code,
    String field,
    String message
) {}
```

**When to use records:**
- Request/Response DTOs
- Value objects with no identity
- Immutable data structures
- Configuration objects

**When NOT to use records:**
- JPA entities (need mutable for JPA)
- Classes with complex behavior
- Classes needing inheritance

---

## Enums

Use enums for fixed sets of values:

```java
public enum MessageType {
    MT200("200", "Financial Institution Transfer");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
```

**Required enums:**
- `MessageType` — MT200, etc.
- `Network` — SWIFT, SEPA
- `MessageStatus` — DRAFT, VALIDATION_FAILED
- `ValidationResult` — SUCCESS, FAILED
- `ErrorCode` — MSG-000 to MSG-007

---

## JPA Entities

```java
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false, length = 50)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status;

    @Column(name = "amount", precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private String validationErrors;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters and setters
}
```

**Entity conventions:**
- Use `@Table(name = "...")` with snake_case table names
- Use `@Column(name = "...")` with snake_case column names
- Use `@Enumerated(EnumType.STRING)` for enums (not ordinal)
- Use `LocalDateTime` for timestamps
- Use `BigDecimal` for monetary values

---

## Services

```java
@Service
@Slf4j
public class MessageCreationService {

    private final MessageRepository messageRepository;
    private final MessageValidationService validationService;

    public MessageCreationService(
            MessageRepository messageRepository,
            MessageValidationService validationService) {
        this.messageRepository = messageRepository;
        this.validationService = validationService;
    }

    public MessageResponse createMessage(MessageRequest request) {
        log.info("Creating message with requestReference: {}", request.requestReference());
        // ...
    }
}
```

**Service conventions:**
- Use constructor injection (not `@Autowired` on fields)
- Log entry points with `log.info()`
- Log validation failures with `log.warn()`
- Throw exceptions for error conditions
- Return DTOs, not entities, to controllers

---

## Controllers

```java
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageCreationService messageCreationService;

    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
            @Valid @RequestBody MessageRequest request,
            HttpServletRequest httpRequest) {
        MessageResponse response = messageCreationService.createMessage(request);
        return ResponseEntity.ok(response);
    }
}
```

**Controller conventions:**
- Use `@RestController` and `@RequestMapping`
- Use `@RequiredArgsConstructor` for constructor injection
- Validate requests with `@Valid`
- Return `ResponseEntity<T>` for explicit status codes
- Extract user info from `HttpServletRequest` or `@AuthenticationPrincipal`

---

## Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ValidationError(
                "MSG-001",
                error.getField(),
                error.getDefaultMessage()))
            .toList();

        return ResponseEntity.badRequest()
            .body(new ErrorResponse(errors));
    }
}
```

**Exception conventions:**
- Use `@RestControllerAdvice` for global handling
- Map exceptions to appropriate HTTP status codes
- Include error codes for client handling
- Log exceptions with correlation ID

---

## Logging

```java
@Slf4j
@Service
public class MessageCreationService {

    public MessageResponse createMessage(MessageRequest request) {
        MDC.put("correlationId", UUID.randomUUID().toString());

        log.info("Creating message: requestReference={}", request.requestReference());

        try {
            MessageResponse response = doCreateMessage(request);
            log.info("Message created: messageId={}", response.messageId());
            return response;
        } catch (Exception e) {
            log.error("Failed to create message: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

**Logging conventions:**
- Use `@Slf4j` (Lombok)
- Use structured logging with key=value pairs
- Use MDC for correlation IDs
- Log levels:
  - `ERROR` — Exceptions, failures
  - `WARN` — Validation failures, recoverable issues
  - `INFO` — Entry points, key business events
  - `DEBUG` — Detailed flow (not in production)

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class MessageCreationServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageValidationService validationService;

    @InjectMocks
    private MessageCreationService messageCreationService;

    @Test
    void createMessage_validRequest_returnsDraftStatus() {
        // Given
        var request = validMessageRequest();
        when(validationService.validate(any())).thenReturn(ValidationResult.success());
        when(messageRepository.save(any())).thenReturn(messageEntity());

        // When
        var response = messageCreationService.createMessage(request);

        // Then
        assertThat(response.status()).isEqualTo(MessageStatus.DRAFT);
        assertThat(response.messageId()).isNotNull();
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class MessageRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void save_validMessage_persistsToDatabase() {
        var message = Message.builder()
            .messageId("MSG-001")
            .status(MessageStatus.DRAFT)
            .build();

        var saved = messageRepository.save(message);

        assertThat(saved.getId()).isNotNull();
    }
}
```

**Testing conventions:**
- Use JUnit 5 (`@Test`, `@ExtendWith`)
- Use Mockito for mocking (`@Mock`, `@InjectMocks`)
- Use AssertJ for assertions (`assertThat(...)`)
- Use Testcontainers for integration tests
- Name tests: `methodName_scenario_expectedResult`
- Follow Given-When-Then structure

---

## Validation

### Bean Validation (Input)

```java
public record MessageRequest(
    @NotBlank(message = "Message type is required")
    String messageType,

    @NotBlank(message = "Network is required")
    String network,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,

    @Pattern(regexp = "^[A-Z]{3}$", message = "Invalid currency code")
    String currency
) {}
```

### Custom Validation

```java
@Component
public class MessageRequestValidator {

    public ValidationResult validate(MessageRequest request) {
        List<ValidationError> errors = new ArrayList<>();

        if (!isValidCurrency(request.currency())) {
            errors.add(new ValidationError("MSG-001", "currency", "Invalid ISO 4217 currency code"));
        }

        if (!isValidValueDate(request.valueDate())) {
            errors.add(new ValidationError("MSG-001", "valueDate", "Invalid date format"));
        }

        return errors.isEmpty()
            ? ValidationResult.success()
            : ValidationResult.failed(errors);
    }
}
```

---

## Error Response Format

```java
public record ErrorResponse(
    String messageId,
    String messageType,
    String network,
    MessageStatus status,
    ValidationResult validationResult,
    List<ValidationError> validationErrors
) {}

public record ValidationError(
    String code,
    String field,
    String message
) {}
```

---

## Code Style

### Formatting

- **Indentation:** 4 spaces (no tabs)
- **Line length:** 120 characters max
- **Braces:** K&R style (opening brace on same line)
- **Blank lines:** One between methods, two between sections

### Imports

- Use explicit imports (no wildcard `.*`)
- Order: java.*, javax.*, jakarta.*, org.*, com.*, static

### Lombok

Allowed annotations:
- `@Slf4j` — Logging
- `@RequiredArgsConstructor` — Constructor injection
- `@Builder` — Builder pattern for entities/DTOs
- `@Getter` / `@Setter` — When needed

**Not allowed:**
- `@Data` (too permissive)
- `@AllArgsConstructor` (prefer explicit constructors)
- `@NoArgsConstructor` on entities (JPA only)

---

## Documentation

### Javadoc

Required for:
- Public APIs
- Service methods
- Complex algorithms
- Non-obvious business logic

```java
/**
 * Creates a financial institution transfer message.
 *
 * @param request the message creation request containing all required fields
 * @return the created message response with generated message ID
 * @throws ValidationException if the request fails validation
 */
public MessageResponse createMessage(MessageRequest request) {
    // ...
}
```

### OpenAPI

```java
@RestController
@Tag(name = "Messages", description = "Financial message operations")
public class MessageController {

    @Operation(summary = "Create a message", description = "Creates a new MT200 message")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(@Valid @RequestBody MessageRequest request) {
        // ...
    }
}
```

---

## Dependencies

### Required

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | JPA/Hibernate |
| `spring-boot-starter-validation` | Bean validation |
| `spring-boot-starter-security` | OAuth 2.0 |
| `postgresql` | Database driver |
| `flyway-core` | Migrations |
| `lombok` | Boilerplate reduction |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI/Swagger |
| `caffeine` | Caching |

### Test

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-test` | Testing |
| `testcontainers` | Integration tests |
| `pact-jvm-consumer-junit5` | Contract tests |

---

## Hard Rules

1. **No magic numbers** — Use constants or enums
2. **No raw strings** — Use constants for error codes, messages
3. **No swallowed exceptions** — Log and rethrow or handle
4. **No System.out** — Use SLF4J
5. **No mutable state in services** — Services should be stateless
6. **No business logic in controllers** — Controllers only delegate
7. **No direct entity exposure** — Always use DTOs in API

---

## References

- Architecture Reference: `.claude/_architecture-reference.md`
- PRD: `docs/PRD.md`
