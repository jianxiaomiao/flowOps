package com.example.demo.ticket;

/** 工单状态机唯一认可的状态。 */
//1. OPEN
//释义：新建、待分派、未处理
//场景：刚提交的工单 / 问题，还没人认领
//2. ASSIGNED
//释义：已分配、已指派
//场景：负责人已确定，分配给对应处理人
//3. ACCEPTED
//释义：已接收、已认领
//场景：处理人确认收到任务，开始跟进
//4. PROCESSING
//释义：处理中、正在解决
//场景：工程师 / 客服正在操作、排查、修复
//5. PENDING_CONFIRMATION
//释义：待确认、等待客户复核
//场景：问题修复完成，等待用户确认是否解决
//6. RESOLVED
//释义：已解决、修复完毕
//场景：处理方判定问题修复，等待收尾关闭
//7. CLOSED
//释义：已关闭、归档完结
//场景：用户确认无误，工单正式结束存档
//完整流转顺序（标准流程）
//OPEN → ASSIGNED → ACCEPTED → PROCESSING → PENDING_CONFIRMATION → RESOLVED → CLOSED
//新建工单 → 分配负责人 → 处理人接收 → 处理问题 → 等待用户确认 → 标记已解决 → 工单关闭
public enum TicketStatus {
    OPEN, ASSIGNED, ACCEPTED, PROCESSING, PENDING_CONFIRMATION, RESOLVED, CLOSED
}
