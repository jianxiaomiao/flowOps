package com.example.demo.ticket;

/** 客户端只能请求动作，目标状态由服务端状态机决定。 */
public enum TicketAction {
    CREATE, ASSIGN, CLAIM, ACCEPT, START_PROCESS, SUBMIT_RESOLUTION, CONFIRM, REJECT, CLOSE
}
