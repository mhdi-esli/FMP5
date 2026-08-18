package com.bank.messaging.dto;

import com.bank.messaging.entity.MessageDefinitionMapping;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageDefinitionResponse mapping from entity.
 */
class MessageDefinitionResponseTest {

    @Test
    void fromEntity_shouldMapAllFields() {
        LocalDateTime now = LocalDateTime.now();
        MessageDefinitionMapping entity = MessageDefinitionMapping.builder()
                .id(1L)
                .messageType("MT200")
                .network("SWIFT")
                .version(1)
                .fieldMappings("{\"field\": \"value\"}")
                .validationRules("{\"rule\": \"active\"}")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        MessageDefinitionResponse response = MessageDefinitionResponse.fromEntity(entity);

        assertEquals(1L, response.id());
        assertEquals("MT200", response.messageType());
        assertEquals("SWIFT", response.network());
        assertEquals(1, response.version());
        assertEquals("{\"field\": \"value\"}", response.fieldMappings());
        assertEquals("{\"rule\": \"active\"}", response.validationRules());
        assertTrue(response.isActive());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void fromEntity_withNullMappings_shouldMapNulls() {
        MessageDefinitionMapping entity = MessageDefinitionMapping.builder()
                .id(2L)
                .messageType("MT202")
                .network("SEPA")
                .version(1)
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .build();

        MessageDefinitionResponse response = MessageDefinitionResponse.fromEntity(entity);

        assertEquals(2L, response.id());
        assertEquals("MT202", response.messageType());
        assertEquals("SEPA", response.network());
        assertFalse(response.isActive());
        assertNull(response.fieldMappings());
        assertNull(response.validationRules());
    }
}
