package com.bank.messaging.controller;

import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.dto.MessageResponse;
import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.service.MessageCreationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for financial message operations.
 */
@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "Financial message operations")
public class MessageController {

    private final MessageCreationService messageCreationService;

    /**
     * Create a new financial institution transfer message.
     *
     * @param request the message creation request
     * @return the created message response
     */
    @PostMapping
    @Operation(summary = "Create a message", description = "Creates a new MT200 financial institution transfer message")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MessageResponse> createMessage(@Valid @RequestBody MessageRequest request) {
        log.info("Received message creation request for reference: {}", request.requestReference());
        MessageResponse response = messageCreationService.createMessage(request);

        if (MessageStatus.DRAFT.equals(response.status())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
