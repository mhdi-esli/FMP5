package com.bank.messaging.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * JPA Entity for message definition mappings.
 * This is a stub for EPIC-02 - will be enhanced when EPIC-02 is implemented.
 */
@Entity
@Table(name = "message_definition_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDefinitionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_type", nullable = false, length = 10)
    private String messageType;

    @Column(name = "network", nullable = false, length = 20)
    private String network;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "field_mappings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String fieldMappings;

    @Column(name = "validation_rules", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String validationRules;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
