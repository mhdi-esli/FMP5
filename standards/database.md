# Database Standards

## Flyway Migrations

**Rule:** All database changes must use Flyway migrations

**Naming Convention:** `V{version}__{description}.sql`

**Examples:**
- `V1__Create_messages_table.sql`
- `V2__Enrich_message_definition_mappings.sql`

**Rules:**
1. All migrations must be idempotent (where possible)
2. Use descriptive names
3. Include comments for complex changes
4. Test migrations locally before committing

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
    
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

**Rules:**
- Use `@Table(name = "...")` with snake_case table names
- Use `@Column(name = "...")` with snake_case column names
- Use `@Enumerated(EnumType.STRING)` for enums
- Use `LocalDateTime` for timestamps
- Use `BigDecimal` for monetary values

---

## JSONB Fields

```java
@Column(name = "field_mappings", columnDefinition = "jsonb")
private String fieldMappings;

@Column(name = "validation_rules", columnDefinition = "jsonb")
private String validationRules;
```

**Rules:**
- Store as raw String
- Validate JSON syntax in service layer
- No structural validation on storage

---

## Repository Queries

```java
public interface MessageDefinitionMappingRepository extends JpaRepository<MessageDefinitionMapping, Long> {
    Optional<MessageDefinitionMapping> findByMessageTypeAndNetworkAndIsActiveTrue(
        String messageType, String network);
    
    List<MessageDefinitionMapping> findByMessageTypeAndNetwork(
        String messageType, String network);
    
    List<MessageDefinitionMapping> findAllByIsActiveTrue();
}
```

**Rules:**
- Use Spring Data naming conventions
- Use `Optional<T>` for single results
- Use `List<T>` for collections
