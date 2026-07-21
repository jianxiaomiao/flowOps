package com.example.demo.messaging;

public final class RabbitTopology {
    public static final String EXCHANGE = "flowops.events";
    public static final String NOTIFICATION_QUEUE = "flowops.notification";
    public static final String KNOWLEDGE_QUEUE = "flowops.knowledge";
    public static final String DEAD_LETTER_EXCHANGE = "flowops.dlx";
    public static final String DEAD_LETTER_QUEUE = "flowops.notification.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "notification.dead";
    private RabbitTopology() { }
}
