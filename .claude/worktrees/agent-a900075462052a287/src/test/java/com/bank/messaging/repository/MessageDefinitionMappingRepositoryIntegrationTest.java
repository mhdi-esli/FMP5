package com.bank.messaging.repository;

import com.bank.messaging.entity.MessageDefinitionMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for MessageDefinitionMappingRepository.
 */
@DataJpaTest(excludeAutoConfiguration = org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class)
@Disabled("Requires Docker-in-Docker, not available in this environment")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MessageDefinitionMappingRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageDefinitionMappingRepository repository;

    private MessageDefinitionMapping sampleDefinition;

    @BeforeEach
    void setUp() {
        sampleDefinition = MessageDefinitionMapping.builder()
                .messageType("MT200")
                .network("SWIFT")
                .version(1)
                .fieldMappings("{\"field\": \"value\"}")
                .validationRules("{\"rule\": \"active\"}")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("saveDefinition_persistsAndRetrieves")
    void saveDefinition_persistsAndRetrieves() {
        MessageDefinitionMapping saved = entityManager.persistFlushFind(sampleDefinition);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMessageType()).isEqualTo("MT200");
        assertThat(saved.getNetwork()).isEqualTo("SWIFT");
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("saveDefinition_fieldMappings_storedAsJsonb")
    void saveDefinition_fieldMappings_storedAsJsonb() {
        sampleDefinition.setFieldMappings("{\"field\": \"value\"}");
        MessageDefinitionMapping saved = entityManager.persistFlushFind(sampleDefinition);
        assertThat(saved.getFieldMappings()).isEqualTo("{\"field\": \"value\"}");
    }

    @Test
    @DisplayName("saveDefinition_validationRules_storedAsJsonb")
    void saveDefinition_validationRules_storedAsJsonb() {
        sampleDefinition.setValidationRules("{\"rule\": \"active\"}");
        MessageDefinitionMapping saved = entityManager.persistFlushFind(sampleDefinition);
        assertThat(saved.getValidationRules()).isEqualTo("{\"rule\": \"active\"}");
    }

    @Test
    @DisplayName("findByTypeAndNetwork_returnsMatching")
    void findByTypeAndNetwork_returnsMatching() {
        entityManager.persist(sampleDefinition);
        Optional<MessageDefinitionMapping> result = repository.findByMessageTypeAndNetwork("MT200", "SWIFT");
        assertThat(result).isPresent();
        assertThat(result.get().getMessageType()).isEqualTo("MT200");
    }

    @Test
    @DisplayName("findActiveDefinition_returnsOnlyActive")
    void findActiveDefinition_returnsOnlyActive() {
        entityManager.persist(sampleDefinition);
        entityManager.persist(MessageDefinitionMapping.builder()
                .messageType("MT200")
                .network("SWIFT")
                .version(2)
                .isActive(false)
                .build());
        Optional<MessageDefinitionMapping> result = repository.findByMessageTypeAndNetworkAndIsActiveTrue("MT200", "SWIFT");
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("uniqueConstraint_duplicate_throwsException")
    void uniqueConstraint_duplicate_throwsException() {
        entityManager.persist(sampleDefinition);
        entityManager.flush();
        MessageDefinitionMapping duplicate = MessageDefinitionMapping.builder()
                .messageType("MT200")
                .network("SWIFT")
                .version(1)
                .isActive(true)
                .build();
        assertThatThrownBy(() -> {
            entityManager.persist(duplicate);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("updateDefinition_modifiesFields")
    void updateDefinition_modifiesFields() {
        MessageDefinitionMapping saved = entityManager.persistFlushFind(sampleDefinition);
        saved.setVersion(2);
        saved.setIsActive(false);
        entityManager.flush();
        MessageDefinitionMapping updated = entityManager.find(MessageDefinitionMapping.class, saved.getId());
        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("softDelete_setsInactive")
    void softDelete_setsInactive() {
        MessageDefinitionMapping saved = entityManager.persistFlushFind(sampleDefinition);
        saved.setIsActive(false);
        entityManager.flush();
        Optional<MessageDefinitionMapping> active = repository.findByMessageTypeAndNetworkAndIsActiveTrue("MT200", "SWIFT");
        assertThat(active).isEmpty();
        Optional<MessageDefinitionMapping> any = repository.findByMessageTypeAndNetwork("MT200", "SWIFT");
        assertThat(any).isPresent();
        assertThat(any.get().getIsActive()).isFalse();
    }

    @Test
    @DisplayName("listAll_returnsAllDefinitions")
    void listAll_returnsAllDefinitions() {
        entityManager.persist(sampleDefinition);
        entityManager.persist(MessageDefinitionMapping.builder()
                .messageType("MT202")
                .network("SEPA")
                .version(1)
                .isActive(true)
                .build());
        List<MessageDefinitionMapping> all = repository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("listFiltered_returnsCorrectSubset")
    void listFiltered_returnsCorrectSubset() {
        entityManager.persist(sampleDefinition);
        entityManager.persist(MessageDefinitionMapping.builder()
                .messageType("MT202")
                .network("SEPA")
                .version(1)
                .isActive(true)
                .build());
        List<MessageDefinitionMapping> swiftDefs = repository.findByNetwork("SWIFT");
        assertThat(swiftDefs).hasSize(1);
        assertThat(swiftDefs.get(0).getNetwork()).isEqualTo("SWIFT");
        List<MessageDefinitionMapping> mt200Defs = repository.findByMessageType("MT200");
        assertThat(mt200Defs).hasSize(1);
        assertThat(mt200Defs.get(0).getMessageType()).isEqualTo("MT200");
        List<MessageDefinitionMapping> activeDefs = repository.findByIsActiveTrue();
        assertThat(activeDefs).hasSize(2);
    }
}
