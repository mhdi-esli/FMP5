package com.bank.messaging.controller;

import com.bank.messaging.dto.MessageDefinitionRequest;
import com.bank.messaging.dto.MessageDefinitionResponse;
import com.bank.messaging.service.MessageDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing message definitions (EPIC-02).
 */
@RestController
@RequestMapping("/api/v1/message-definitions")
@RequiredArgsConstructor
@Tag(name = "Message Definitions", description = "CRUD operations for message definition mappings")
public class MessageDefinitionController {

    private final MessageDefinitionService definitionService;

    /**
     * Create a new message definition.
     */
    @PostMapping
    @Operation(summary = "Create message definition", description = "Creates a new message definition mapping")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Definition created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Duplicate definition (MSG-008)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MessageDefinitionResponse> createDefinition(
            @Valid @RequestBody MessageDefinitionRequest request) {
        MessageDefinitionResponse response = definitionService.createDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a message definition by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get message definition by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Definition found"),
        @ApiResponse(responseCode = "404", description = "Definition not found (MSG-009)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MessageDefinitionResponse> getDefinition(@PathVariable Long id) {
        MessageDefinitionResponse response = definitionService.getDefinition(id);
        return ResponseEntity.ok(response);
    }

    /**
     * List message definitions with optional filters.
     */
    @GetMapping
    @Operation(summary = "List message definitions", description = "List definitions with optional filters: messageType, network, isActive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of definitions"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<MessageDefinitionResponse>> listDefinitions(
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) Boolean isActive) {
        List<MessageDefinitionResponse> responses = definitionService.listDefinitions(messageType, network, isActive);
        return ResponseEntity.ok(responses);
    }

    /**
     * Update an existing message definition.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update message definition")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Definition updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Definition not found (MSG-009)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MessageDefinitionResponse> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody MessageDefinitionRequest request) {
        MessageDefinitionResponse response = definitionService.updateDefinition(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-delete a message definition.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete message definition", description = "Soft-delete (deactivates) a message definition")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Definition deactivated"),
        @ApiResponse(responseCode = "404", description = "Definition not found (MSG-009)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        definitionService.deleteDefinition(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lookup an active definition by message type and network.
     * Used internally by EPIC-01 for message creation.
     */
    @GetMapping("/lookup")
    @Operation(summary = "Lookup active definition", description = "Find active definition by message type and network (used by EPIC-01)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active definition found"),
        @ApiResponse(responseCode = "404", description = "No active definition found (MSG-004/MSG-005)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MessageDefinitionResponse> lookupDefinition(
            @RequestParam String messageType,
            @RequestParam String network) {
        return definitionService.findActiveDefinition(messageType, network)
                .map(entity -> ResponseEntity.ok(MessageDefinitionResponse.fromEntity(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
}
