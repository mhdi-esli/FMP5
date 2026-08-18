package com.bank.messaging.entity;

import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.enums.Network;
import com.bank.messaging.enums.ValidationResultEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for financial messages.
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false, length = 50)
    private String messageId;

    @Column(name = "message_type", nullable = false, length = 10)
    private String messageType;

    @Column(name = "network", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Network network;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Column(name = "request_reference", length = 100)
    private String requestReference;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "related_reference", length = 100)
    private String relatedReference;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "amount", precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "sender_institution_id", length = 50)
    private String senderInstitutionId;

    @Column(name = "sender_bic", length = 11)
    private String senderBic;

    @Column(name = "sender_branch_id", length = 50)
    private String senderBranchId;

    @Column(name = "receiver_institution_id", length = 50)
    private String receiverInstitutionId;

    @Column(name = "receiver_bic", length = 11)
    private String receiverBic;

    @Column(name = "receiver_branch_id", length = 50)
    private String receiverBranchId;

    @Column(name = "charge_type", length = 3)
    private String chargeType;

    @Column(name = "instruction_code", length = 10)
    private String instructionCode;

    @Column(name = "narrative", columnDefinition = "TEXT")
    private String narrative;

    @Column(name = "additional_information", columnDefinition = "TEXT")
    private String additionalInformation;

    @Column(name = "validation_result", length = 20)
    @Enumerated(EnumType.STRING)
    private ValidationResultEnum validationResult;

    @Column(name = "validation_errors", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String validationErrors;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
