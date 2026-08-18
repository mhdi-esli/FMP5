package com.bank.messaging.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FieldMapping record structure.
 */
class FieldMappingTest {

    @Test
    void shouldCreateFieldMappingWithAllFields() {
        FieldMapping mapping = new FieldMapping("amount", ":32A:", "Amount", true, 20, "decimal", "^[0-9]+\\.[0-9]{2}$");

        assertEquals("amount", mapping.fieldName());
        assertEquals(":32A:", mapping.swiftTag());
        assertEquals("Amount", mapping.sepaField());
        assertTrue(mapping.required());
        assertEquals(20, mapping.maxLength());
        assertEquals("decimal", mapping.dataType());
        assertEquals("^[0-9]+\\.[0-9]{2}$", mapping.validationPattern());
    }

    @Test
    void shouldCreateFieldMappingWithOptionalFieldsNull() {
        FieldMapping mapping = new FieldMapping("narrative", ":50:", "Narrative", false, null, "string", null);

        assertEquals("narrative", mapping.fieldName());
        assertFalse(mapping.required());
        assertNull(mapping.maxLength());
        assertNull(mapping.validationPattern());
    }

    @Test
    void equalsAndHashCode_shouldWork() {
        FieldMapping m1 = new FieldMapping("amount", ":32A:", "Amount", true, 20, "decimal", null);
        FieldMapping m2 = new FieldMapping("amount", ":32A:", "Amount", true, 20, "decimal", null);

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
