package com.bank.messaging.service;

import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.dto.MessageResponse;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.entity.Message;
import com.bank.messaging.enums.*;
import com.bank.messaging.repository.MessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for creating financial institution transfer messages.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageCreationService {

    private final MessageRepository messageRepository;
    private final MessageValidationService validationService;
    private final MessageDefinitionService messageDefinitionService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MESSAGE_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicInteger dailySequence = new AtomicInteger(1);

    /**
     * Creates a financial institution transfer message.
     *
     * @param request the message creation request
     * @return the message response
     */
    @Transactional
    public MessageResponse createMessage(MessageRequest request) {
        log.info("Creating message with requestReference: {}", request.requestReference());

        // Validate request
        ValidationResult validation = validationService.validate(request);

        if (!validation.isSuccess()) {
            log.warn("Validation failed for request: {}", request.requestReference());
            return createFailedResponse(request, validation.errors());
        }

        // Validate network support (MSG-003)
        Network network = Network.fromCode(request.network());

        // Check message definition exists and is active (MSG-004, MSG-005)
        ValidationResult definitionValidation = validateMessageDefinition(request.messageType(), network);
        if (!definitionValidation.isSuccess()) {
            return createFailedResponse(request, definitionValidation.errors());
        }

        // Generate message ID
        String messageId = generateMessageId();

        // Create and save message entity
        Message message = buildMessageEntity(request, messageId, network);
        message = messageRepository.save(message);

        log.info("Message created successfully: messageId={}", messageId);

        return buildSuccessResponse(message);
    }

    private ValidationResult validateMessageDefinition(String messageType, Network network) {
        var definitionOpt = messageDefinitionService
            .findActiveDefinition(messageType, network.name());

        if (definitionOpt.isEmpty()) {
            // Check if definition exists but is inactive
            var anyDefinitionOpt = messageDefinitionService
                .findDefinition(messageType, network.name());

            if (anyDefinitionOpt.isPresent()) {
                log.warn("Message Definition inactive for type: {}, network: {}", messageType, network);
                return ValidationResult.failed(new ValidationError(
                    ErrorCode.MSG_005.getCode(),
                    "messageDefinition",
                    ErrorCode.MSG_005.getPersianMessage()
                ));
            }

            log.warn("Message Definition not found for type: {}, network: {}", messageType, network);
            return ValidationResult.failed(new ValidationError(
                ErrorCode.MSG_004.getCode(),
                "messageDefinition",
                ErrorCode.MSG_004.getPersianMessage()
            ));
        }

        return ValidationResult.success();
    }

    private String generateMessageId() {
        String datePart = LocalDateTime.now().format(MESSAGE_ID_DATE_FORMAT);
        int sequence = dailySequence.getAndIncrement();
        return String.format("MSG-%s-%06d", datePart, sequence);
    }

    private Message buildMessageEntity(MessageRequest request, String messageId, Network network) {
        LocalDate valueDate = LocalDate.parse(request.valueDate(), DATE_FORMATTER);

        return Message.builder()
            .messageId(messageId)
            .messageType(request.messageType())
            .network(network)
            .status(MessageStatus.DRAFT)
            .requestReference(request.requestReference())
            .transactionReference(request.transactionReference())
            .relatedReference(request.relatedReference())
            .valueDate(valueDate)
            .currency(request.currency())
            .amount(request.amount())
            .senderInstitutionId(request.senderInstitutionIdentifier())
            .senderBic(request.senderBIC())
            .senderBranchId(request.senderBranchIdentifier())
            .receiverInstitutionId(request.receiverInstitutionIdentifier())
            .receiverBic(request.receiverBIC())
            .receiverBranchId(request.receiverBranchIdentifier())
            .chargeType(request.chargeType())
            .instructionCode(request.instructionCode())
            .narrative(request.narrative())
            .additionalInformation(request.additionalInformation())
            .validationResult(ValidationResultEnum.SUCCESS)
            .build();
    }

    private MessageResponse createFailedResponse(MessageRequest request, List<ValidationError> errors) {
        // Save failed message attempt
        String messageId = null;
        Message message = Message.builder()
            .messageType(request.messageType())
            .network(Network.fromCode(request.network()))
            .status(MessageStatus.VALIDATION_FAILED)
            .requestReference(request.requestReference())
            .transactionReference(request.transactionReference())
            .validationResult(ValidationResultEnum.FAILED)
            .validationErrors(toJson(errors))
            .build();
        message = messageRepository.save(message);

        Network network = Network.fromCode(request.network());

        return new MessageResponse(
            null,
            request.messageType(),
            network != null ? network.name() : request.network(),
            MessageStatus.VALIDATION_FAILED,
            LocalDateTime.now(),
            ValidationResultEnum.FAILED,
            errors
        );
    }

    private String toJson(List<ValidationError> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize validation errors", e);
            return "[]";
        }
    }

    private MessageResponse buildSuccessResponse(Message message) {
        return new MessageResponse(
            message.getMessageId(),
            message.getMessageType(),
            message.getNetwork().name(),
            message.getStatus(),
            message.getCreatedAt(),
            message.getValidationResult(),
            Collections.emptyList()
        );
    }
}
