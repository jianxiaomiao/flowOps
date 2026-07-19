package com.example.demo.ticket;

/** 工单状态机唯一认可的状态。 */
public enum TicketStatus {
    OPEN, ASSIGNED, ACCEPTED, PROCESSING, PENDING_CONFIRMATION, RESOLVED, CLOSED
}
