package com.bank.messaging.service;

import com.bank.messaging.dto.MessageDefinitionRequest;
import com.bank.messaging.dto.MessageDefinitionResponse;
import com.bank.messaging.entity.MessageDefinitionMapping;
import com.bank.messaging.exception.DefinitionNotFoundException;
import com.bank.messaging.exception.DuplicateDefinitionException;
import com.bank.messaging.exception.InvalidJsonException;
import com.bank.messaging.repository.MessageDefinitionMappingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing message definition mappings (EPIC-02).
 * Provides CRUD operations and an active-definition lookup used by EPIC-01.
 */
@Service
@Slf4j
public class MessageDefinitionService {

    private final MessageDefinitionMappingRepository repository;
    private final ObjectMapper objectMapper;

    public MessageDefinitionService(MessageDefinitionMappingRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a new message definition.
     *
     * @param request the definition details
     * @return the created definition
     * @throws DuplicateDefinitionException if the same messageType + network + version already exists
     */
    @Transactional
    @CacheEvict(value = "messageDefinitions", allEntries = true)
    public MessageDefinitionResponse createDefinition(MessageDefinitionRequest request) {
        log.info("Creating message definition: {}/{}/{}", request.messageType(), request.network(), request.version());

        Optional<MessageDefinitionMapping> existing = repository.findByMessageTypeAndNetwork(
                request.messageType(), request.network());
        if (existing.isPresent()) {
            throw new DuplicateDefinitionException(request.messageType(), request.network(), request.version());
        }

        validateJsonFormat(request.fieldMappings(), "fieldMappings");
        validateJsonFormat(request.validationRules(), "validationRules");

        MessageDefinitionMapping entity = MessageDefinitionMapping.builder()
                .messageType(request.messageType())
                .network(request.network())
                .version(request.version())
                .fieldMappings(request.fieldMappings())
                .validationRules(request.validationRules())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();

        MessageDefinitionMapping saved = repository.save(entity);
        log.info("Created message definition with id: {}", saved.getId());
        return MessageDefinitionResponse.fromEntity(saved);
    }

    /**
     * Get a message definition by ID.
     *
     * @param id the definition ID
     * @return the definition
     * @throws DefinitionNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public MessageDefinitionResponse getDefinition(Long id) {
        MessageDefinitionMapping entity = repository.findById(id)
                .orElseThrow(() -> new DefinitionNotFoundException(id));
        return MessageDefinitionResponse.fromEntity(entity);
    }

    /**
     * List message definitions with optional filters.
     *
     * @param messageType optional filter by message type
     * @param network     optional filter by network
     * @param isActive    optional filter by active status
     * @return filtered list of definitions
     */
    @Transactional(readOnly = true)
    public List<MessageDefinitionResponse> listDefinitions(String messageType, String network, Boolean isActive) {
        List<MessageDefinitionMapping> entities;

        if (messageType != null && network != null) {
            entities = repository.findAllByMessageTypeAndNetwork(messageType, network);
        } else if (messageType != null) {
            entities = repository.findByMessageType(messageType);
        } else if (network != null) {
            entities = repository.findByNetwork(network);
        } else if (isActive != null && isActive) {
            entities = repository.findByIsActiveTrue();
        } else {
            entities = repository.findAll();
        }

        return entities.stream()
                .map(MessageDefinitionResponse::fromEntity)
                .toList();
    }

    /**
     * Update an existing message definition.
     *
     * @param id      the definition ID
     * @param request the updated fields
     * @return the updated definition
     * @throws DefinitionNotFoundException if not found
     */
    @Transactional
    @CacheEvict(value = "messageDefinitions", allEntries = true)
    public MessageDefinitionResponse updateDefinition(Long id, MessageDefinitionRequest request) {
        log.info("Updating message definition id: {}", id);

        validateJsonFormat(request.fieldMappings(), "fieldMappings");
        validateJsonFormat(request.validationRules(), "validationRules");

        MessageDefinitionMapping entity = repository.findById(id)
                .orElseThrow(() -> new DefinitionNotFoundException(id));

        if (request.messageType() != null) {
            entity.setMessageType(request.messageType());
        }
        if (request.network() != null) {
            entity.setNetwork(request.network());
        }
        if (request.version() != null) {
            entity.setVersion(request.version());
        }
        if (request.fieldMappings() != null) {
            entity.setFieldMappings(request.fieldMappings());
        }
        if (request.validationRules() != null) {
            entity.setValidationRules(request.validationRules());
        }
        if (request.isActive() != null) {
            entity.setIsActive(request.isActive());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        MessageDefinitionMapping saved = repository.save(entity);
        return MessageDefinitionResponse.fromEntity(saved);
    }

    /**
     * Soft-delete a message definition by setting isActive to false.
     *
     * @param id the definition ID
     * @throws DefinitionNotFoundException if not found
     */
    @Transactional
    @CacheEvict(value = "messageDefinitions", allEntries = true)
    public void deleteDefinition(Long id) {
        log.info("Soft-deleting message definition id: {}", id);

        MessageDefinitionMapping entity = repository.findById(id)
                .orElseThrow(() -> new DefinitionNotFoundException(id));

        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
    }

    /**
     * Validate that a JSON string is syntactically valid.
     *
     * @param json      the JSON string to validate (null or empty is skipped)
     * @param fieldName the field name for error reporting
     * @throws InvalidJsonException if the JSON is malformed
     */
    private void validateJsonFormat(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new InvalidJsonException(fieldName);
        }
    }

    /**
     * Find an active message definition by message type and network.
     * Used by EPIC-01 MessageCreationService. Cached with short TTL.
     *
     * @param messageType the message type
     * @param network     the network
     * @return the active definition, if found
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "messageDefinitions", key = "#messageType + ':' + #network", unless = "#result.isEmpty()")
    public Optional<MessageDefinitionMapping> findActiveDefinition(String messageType, String network) {
        return repository.findByMessageTypeAndNetworkAndIsActiveTrue(messageType, network);
    }

    /**
     * Find any message definition (active or inactive) by message type and network.
     * Used by EPIC-01 to distinguish MSG-004 (not found) from MSG-005 (inactive).
     *
     * @param messageType the message type
     * @param network     the network
     * @return the definition, if found
     */
    @Transactional(readOnly = true)
    public Optional<MessageDefinitionMapping> findDefinition(String messageType, String network) {
        return repository.findByMessageTypeAndNetwork(messageType, network);
    }
}
