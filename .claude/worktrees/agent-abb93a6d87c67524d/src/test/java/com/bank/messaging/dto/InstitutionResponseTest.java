package com.bank.messaging.dto;

import com.bank.messaging.entity.Institution;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InstitutionResponse mapping from entity.
 */
class InstitutionResponseTest {

    @Test
    void fromEntity_shouldMapAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Institution entity = Institution.builder()
                .id(1L)
                .institutionId("BANK01")
                .bic("BANK01XXX")
                .name("Test Bank")
                .branchId("BR001")
                .isActive(true)
                .supportedNetworks(List.of("SWIFT", "SEPA"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        InstitutionResponse response = InstitutionResponse.fromEntity(entity);

        assertEquals(1L, response.id());
        assertEquals("BANK01", response.institutionId());
        assertEquals("BANK01XXX", response.bic());
        assertEquals("Test Bank", response.name());
        assertEquals("BR001", response.branchId());
        assertTrue(response.isActive());
        assertEquals(List.of("SWIFT", "SEPA"), response.supportedNetworks());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void fromEntity_withNullOptionals_shouldMapNulls() {
        Institution entity = Institution.builder()
                .id(2L)
                .institutionId("BANK02")
                .name("Bank Two")
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .build();

        InstitutionResponse response = InstitutionResponse.fromEntity(entity);

        assertEquals(2L, response.id());
        assertEquals("BANK02", response.institutionId());
        assertNull(response.bic());
        assertNull(response.branchId());
        assertFalse(response.isActive());
        assertNull(response.supportedNetworks());
    }
}
