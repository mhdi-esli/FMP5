package com.bank.messaging.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Entity for financial institutions.
 * This is a stub for EPIC-03 - will be enhanced when EPIC-03 is implemented.
 */
@Entity
@Table(name = "institutions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", unique = true, nullable = false, length = 50)
    private String institutionId;

    @Column(name = "bic", length = 11)
    private String bic;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "branch_id", length = 50)
    private String branchId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "supported_networks")
    @ElementCollection
    private List<String> supportedNetworks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
