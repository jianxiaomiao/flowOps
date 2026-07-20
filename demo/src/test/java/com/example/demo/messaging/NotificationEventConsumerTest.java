package com.example.demo.messaging;

import com.example.demo.entity.Notification;
import com.example.demo.entity.ProcessedEvent;
import com.example.demo.mapper.NotificationMapper;
import com.example.demo.mapper.ProcessedEventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {
    @Mock ProcessedEventMapper processedEventMapper;
    @Mock NotificationMapper notificationMapper;

    @Test
    void duplicateDeliveryShouldCreateNotificationOnlyOnce() throws Exception {
        when(processedEventMapper.insertIfAbsent(any(ProcessedEvent.class))).thenReturn(1, 0);
        NotificationEventConsumer consumer = new NotificationEventConsumer(
                processedEventMapper, notificationMapper, new ObjectMapper());
        String message = """
                {"eventId":"e1","tenantId":1,"aggregateType":"TICKET","aggregateId":9,
                 "eventType":"TICKET_ASSIGNED","payload":{"ticketNo":"T-1","title":"故障","recipientIds":[7]}}
                """;

        consumer.consume(message);
        consumer.consume(message);

        verify(notificationMapper, times(1)).insert(any(Notification.class));
    }
}
