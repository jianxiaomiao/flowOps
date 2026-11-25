package com.example.demo.integration;

import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/integration/v1/tickets")
class ExternalTicketController {
    private static final Set<String> FIELDS = Set.of("schemaVersion", "eventId", "externalTicketId",
            "title", "description", "submittedAt", "applicationId");

    private final ExternalTicketService service;
    private final ObjectMapper json;
    private final Validator validator;

    ExternalTicketController(ExternalTicketService service, ObjectMapper json, Validator validator) {
        this.service = service;
        this.json = json;
        this.validator = validator;
    }

    @PostMapping
    ResponseEntity<Ack> ingest(@AuthenticationPrincipal TicketIntegration integration, @RequestBody JsonNode body,
                               @RequestParam MultiValueMap<String, String> query) {
        if (!body.isObject() || !query.isEmpty()) return ResponseEntity.badRequest().build();
        var fields = body.fieldNames();
        while (fields.hasNext()) {
            if (!FIELDS.contains(fields.next())) return ResponseEntity.badRequest().build();
        }
        Request request;
        try {
            request = json.treeToValue(body, Request.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException invalid) {
            return ResponseEntity.badRequest().build();
        }
        if (!validator.validate(request).isEmpty()) return ResponseEntity.badRequest().build();
        var result = service.importTicket(integration, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.ack());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<Void> invalidJson() {
        return ResponseEntity.badRequest().build();
    }

    @com.fasterxml.jackson.annotation.JsonPropertyOrder({"schemaVersion", "eventId", "externalTicketId",
            "title", "description", "submittedAt", "applicationId"})
    record Request(@Min(1) @Max(1) int schemaVersion, @NotNull UUID eventId, @NotNull UUID externalTicketId,
                   @NotBlank @Size(max = 200) String title,
                   @NotBlank @Size(max = 8000) String description,
                   @NotNull Instant submittedAt, @Positive Long applicationId) {}

    record Ack(UUID eventId, UUID externalTicketId, String flowOpsTicketId, String ticketNo) {}
}
