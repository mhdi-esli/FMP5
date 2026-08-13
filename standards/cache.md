# Cache Standards

## Caffeine Configuration

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=300s
```

**Rules:**
- Maximum size: 1000 entries
- TTL: 300 seconds (5 minutes)
- Use `@Cacheable` for read operations
- Use `@CacheEvict` for write operations

---

## Caching Annotations

### @Cacheable

```java
@Cacheable(value = "messageDefinitions", key = "{#messageType, #network}")
public Optional<MessageDefinitionMapping> findActiveDefinition(
    String messageType, String network) {
    return repository.findByMessageTypeAndNetworkAndIsActiveTrue(
        messageType, network);
}
```

**Use for:** Read-heavy operations with expensive computation

### @CacheEvict

```java
@CacheEvict(value = "messageDefinitions", allEntries = true)
public MessageDefinitionMapping createDefinition(
    MessageDefinitionRequest request) {
    // ... implementation
}
```

**Use for:** Write operations that invalidate cached data

---

## Cache Keys

**Rule:** Use composite keys for multi-parameter lookups

```java
@Cacheable(value = "messageDefinitions", 
    key = "{#messageType, #network}")
```

**Example:** `findActiveDefinition("MT200", "SWIFT")` → cache key: `["MT200", "SWIFT"]`

---

## Cache Names

Use descriptive cache names:

| Cache Name | Purpose |
|------------|---------|
| `messageDefinitions` | Active message definition lookups |
| `institutions` | Institution master data |
| `messageTypes` | Message type metadata |
