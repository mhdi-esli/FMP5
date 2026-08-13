# Coding Conventions

## Project Structure

```
com.bank.messaging/
├── controller/       # REST endpoints
├── service/          # Business logic
├── repository/       # Data access (Spring Data JPA)
├── entity/           # JPA entities
├── dto/              # Request/Response DTOs
├── enums/            # Enums (MessageType, Network, Status, etc.)
├── config/           # Spring configuration
├── exception/        # Custom exceptions, @ControllerAdvice
└── util/             # Helpers
```

**Verification Method:** Check package structure

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

### Methods

| Type | Convention | Example |
|------|------------|---------|
| Service (create) | `create{Entity}` | `createMessage()` |
| Service (find) | `find{Entity}By{Criteria}` | `findMessageById()` |

### Variables

- Local: `camelCase` (`messageId`, `validationResult`)
- Constants: `SCREAMING_SNAKE_CASE` (`MAX_RETRY_COUNT`)

**Verification Method:** Check class/method names

---

## Java Records

Use records for immutable data carriers:

```java
public record MessageRequest(
    String messageType,
    String network
) {}
```

**When to use:** DTOs, value objects, immutable data

**When NOT to use:** JPA entities, classes with complex behavior

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
}
```

**Required enums:** MessageType, Network, Status, ErrorCode

---

## JPA Entities

```java
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status;
    
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

**Rules:**
- Use `@Table` with snake_case names
- Use `@Column` with snake_case names
- Use `@Enumerated(EnumType.STRING)` for enums
- Use `LocalDateTime` for timestamps
- Use `BigDecimal` for monetary values

---

## Services

```java
@Service
@Slf4j
public class MessageCreationService {
    private final MessageRepository messageRepository;
    
    public MessageCreationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }
}
```

**Rules:**
- Constructor injection only (no `@Autowired` on fields)
- Use `@Slf4j` for logging
- Log entry points with `log.info()`
- Throw exceptions for error conditions
- Return DTOs, not entities

---

## Controllers

```java
@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageCreationService messageCreationService;
    
    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
        @Valid @RequestBody MessageRequest request
    ) {
        return ResponseEntity.ok(messageCreationService.createMessage(request));
    }
}
```

**Rules:**
- Use `@RestController`
- Use `@RequiredArgsConstructor` for injection
- Validate with `@Valid`
- Return `ResponseEntity<T>` for status codes
- No business logic in controllers

---

## Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MessageValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
        MessageValidationException ex
    ) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(ex.getErrors()));
    }
}
```

**Rules:**
- Use `@RestControllerAdvice`
- Map exceptions to appropriate HTTP status codes
- Include error codes in responses

---

## Testing

### Test Naming

Format: `methodName_scenario_expectedResult`

Examples:
- `createMessage_validRequest_returns201`
- `findMessageById_nonExistentId_returnsEmpty`
- `getMessageDefinitions_filtered_returnsFiltered`

### Test Types

| Layer | Target | Tools |
|-------|--------|-------|
| Unit (Service) | ~70% | JUnit 5 + Mockito |
| Unit (Controller) | ~20% | Spring WebMvcTest |
| Integration | ~10% | Testcontainers + SpringBootTest |

---

## Lombok Policy

**Allowed:**
- `@Slf4j` — Logging
- `@RequiredArgsConstructor` — Constructor injection
- `@Builder` — Builder pattern
- `@Getter` / `@Setter` — When needed

**Forbidden:**
- `@Data` — Too permissive
- `@AllArgsConstructor` — Prefer explicit constructors
- `@NoArgsConstructor` on entities

---

## Hard Rules

1. **No magic numbers** — Use constants or enums
2. **No raw strings** — Use constants for error codes
3. **No swallowed exceptions** — Log and rethrow
4. **No System.out** — Use SLF4J
5. **No mutable state in services** — Services should be stateless
6. **No business logic in controllers** — Controllers only delegate
7. **No direct entity exposure** — Always use DTOs in API
