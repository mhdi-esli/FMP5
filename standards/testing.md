# Testing Standards

## Testing Pyramid

| Layer | Target | Tools |
|-------|--------|-------|
| Unit (Service) | ~70% | JUnit 5 + Mockito |
| Unit (Controller) | ~20% | Spring WebMvcTest |
| Integration | ~10% | Testcontainers + SpringBootTest |

---

## Test Naming Conventions

Format: `methodName_scenario_expectedResult`

**Service Tests:**
- `createDefinition_success_returnsCreatedDefinition`
- `getDefinition_existingId_returnsDefinition`
- `findActiveDefinition_notFound_returnsEmpty`

**Controller Tests:**
- `postMessageDefinition_valid_returns201`
- `getMessageDefinition_existing_returns200`
- `deleteMessageDefinition_nonExistent_returns404`

**Integration Tests:**
- `saveDefinition_persistsAndRetrieves`
- `findByTypeAndNetwork_returnsMatching`

---

## Unit Test Requirements

### Service Tests

For each service method, test:
1. Success path with valid input
2. Error paths (exceptions, invalid state)
3. Boundary cases

**Example:**
```java
@Test
void createDefinition_success_returnsCreatedDefinition() {
    // Given
    var request = validRequest();
    
    // When
    var result = service.createDefinition(request);
    
    // Then
    assertNotNull(result.getId());
    assertEquals(request.messageType(), result.messageType());
}
```

---

## Controller Test Requirements

### WebMvcTest Setup

```java
@WebMvcTest(MessageDefinitionController.class)
@AutoConfigureMockMvc
class MessageDefinitionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MessageDefinitionService service;
}
```

### Test Cases

For each endpoint, test:
1. Valid request returns correct status
2. Invalid request returns 400
3. Missing auth returns 401
4. Resource not found returns 404

---

## Integration Test Requirements

### Testcontainers Setup

```java
@SpringBootTest
@Testcontainers
class RepositoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Test Cases

- CRUD operations
- Constraint validation
- JSONB field persistence
- Filtering and search

---

## Test Coverage Requirements

| Component | Minimum Coverage |
|-----------|------------------|
| Service methods | 80% line coverage |
| Controller endpoints | All endpoints tested |
| Integration paths | Happy path + errors |

---

## Persian Message Validation

**Rule:** All user-facing messages must be in Persian

**Test Example:**
```java
@Test
void createDefinition_duplicate_returns409_withPersianMessage() {
    // Given duplicate definition
    
    // When
    var response = mockMvc.perform(post("/v1/message-definitions")
        .contentType(APPLICATION_JSON)
        .body(requestJson));
    
    // Then
    response.andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorList[0].code").value("MSG-008"))
        .andExpect(jsonPath("$.message").value(containsString("تکراری")));
}
```

---

## Test Data Setup

**Use factory methods:**
```java
private MessageDefinitionRequest validRequest() {
    return new MessageDefinitionRequest(
        "MT200",
        "SWIFT",
        "1.0",
        "[]",
        "[]",
        true
    );
}
```
