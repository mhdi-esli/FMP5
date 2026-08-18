package com.bank.messaging.dto;

/**
 * Value object representing a single field mapping definition.
 * Used within the field_mappings JSONB structure.
 * Stored as raw JSON string per TQ-1; this record is for consumer convenience.
 */
public record FieldMapping(
    String fieldName,
    String swiftTag,
    String sepaField,
    boolean required,
    Integer maxLength,
    String dataType,
    String validationPattern
) {}
