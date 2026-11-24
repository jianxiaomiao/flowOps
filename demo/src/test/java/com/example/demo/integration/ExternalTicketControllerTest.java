package com.example.demo.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExternalTicketControllerTest {
    private final ExternalTicketService service = mock(ExternalTicketService.class);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final TicketIntegration integration = new TicketIntegration("nexus-a", 11, 12, 13, "P2");
    private final ExternalTicketController.Request request = new ExternalTicketController.Request(1,
            UUID.randomUUID(), UUID.randomUUID(), "Title", "Body", Instant.parse("2026-11-24T00:00:00Z"), null);

    @Test
    void mapsFirstImportAndReplayToTheirAckStatuses() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var controller = new ExternalTicketController(service, json, factory.getValidator());
            var ack = new ExternalTicketController.Ack(request.eventId(), request.externalTicketId(), "42", "OPS-20261124-0001");
            when(service.importTicket(integration, request))
                    .thenReturn(new ExternalTicketService.Result(true, ack), new ExternalTicketService.Result(false, ack));
            var created = controller.ingest(integration, json.valueToTree(request), new LinkedMultiValueMap<>());
            var replayed = controller.ingest(integration, json.valueToTree(request), new LinkedMultiValueMap<>());
            assertEquals(201, created.getStatusCode().value());
            assertEquals(200, replayed.getStatusCode().value());
            assertEquals(ack, created.getBody());
            assertEquals(ack, replayed.getBody());
        }
    }

    @Test
    void rejectsClientRoutingInBodyOrQueryBeforeImport() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var controller = new ExternalTicketController(service, json, factory.getValidator());
            var query = new LinkedMultiValueMap<String, String>();
            query.add("tenantId", "999");
            var body = json.<com.fasterxml.jackson.databind.node.ObjectNode>valueToTree(request);
            assertEquals(400, controller.ingest(integration, body, query).getStatusCode().value());
            body.put("departmentId", 999);
            assertEquals(400, controller.ingest(integration, body, new LinkedMultiValueMap<>()).getStatusCode().value());
            verifyNoInteractions(service);
        }
    }
}
