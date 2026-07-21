-- FlowOps initial schema. Generated from the reviewed development schema; contains no business/test data.
-- Existing non-empty databases are baselined at V1 and will not execute these CREATE statements.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `audit_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审计记录ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `module` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模块',
  `operation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `resource_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资源类型',
  `resource_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '资源ID',
  `operator_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '操作人',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `request_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求IP（支持IPv6）',
  `result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作结果：SUCCESS、FAILURE',
  `before_data` json NULL COMMENT '操作前关键数据',
  `after_data` json NULL COMMENT '操作后关键数据',
  `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败摘要',
  `occurred_at` datetime(3) NOT NULL COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_resource`(`tenant_id` ASC, `resource_type` ASC, `resource_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_audit_operator`(`tenant_id` ASC, `operator_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_audit_trace`(`trace_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作审计日志表' ROW_FORMAT = Dynamic;

CREATE TABLE `department`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `parent_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '上级部门；根部门为空',
  `department_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户内部门代码',
  `department_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `leader_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '部门负责人',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示顺序',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态：ACTIVE、DISABLED',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  `created_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间',
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '修改人',
  `version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_department_code`(`tenant_id` ASC, `department_code` ASC) USING BTREE,
  INDEX `idx_department_parent`(`tenant_id` ASC, `parent_id` ASC, `sort_order` ASC) USING BTREE,
  INDEX `idx_department_leader`(`tenant_id` ASC, `leader_user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

CREATE TABLE `notification`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `recipient_id` bigint UNSIGNED NOT NULL COMMENT '接收用户',
  `notification_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联业务类型',
  `business_id` bigint UNSIGNED NOT NULL COMMENT '关联业务ID',
  `source_event_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '来源事件ID',
  `read_at` datetime(3) NULL DEFAULT NULL COMMENT '已读时间（空=未读）',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notification_event`(`tenant_id` ASC, `recipient_id` ASC, `source_event_id` ASC, `notification_type` ASC) USING BTREE,
  INDEX `idx_notification_inbox`(`tenant_id` ASC, `recipient_id` ASC, `read_at` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内通知表' ROW_FORMAT = Dynamic;

CREATE TABLE `outbox_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '数据库记录ID',
  `event_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对外事件UUID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `aggregate_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '聚合根类型',
  `aggregate_id` bigint UNSIGNED NOT NULL COMMENT '聚合根ID',
  `event_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型',
  `payload` json NOT NULL COMMENT '事件负载',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发布状态',
  `retry_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_at` datetime(3) NULL DEFAULT NULL COMMENT '下次重试时间',
  `published_at` datetime(3) NULL DEFAULT NULL COMMENT '成功发布时间',
  `last_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近失败摘要',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_outbox_event_id`(`event_id` ASC) USING BTREE,
  INDEX `idx_outbox_publish`(`status` ASC, `next_retry_at` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_outbox_aggregate`(`tenant_id` ASC, `aggregate_type` ASC, `aggregate_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '待发布领域事件表' ROW_FORMAT = Dynamic;

CREATE TABLE `permission`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码，如 ticket:assign',
  `permission_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '中文名称',
  `resource` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资源，如 ticket',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作，如 assign',
  `scope` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限范围：OWN、DEPARTMENT、TENANT',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限说明',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态：ACTIVE、DISABLED',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_permission_code`(`permission_code` ASC) USING BTREE,
  INDEX `idx_permission_resource`(`resource` ASC, `action` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台权限定义表' ROW_FORMAT = Dynamic;

CREATE TABLE `processed_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `event_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件ID',
  `consumer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消费者名称',
  `processed_at` datetime(3) NOT NULL COMMENT '成功处理时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_processed_event`(`consumer_name` ASC, `event_id` ASC) USING BTREE,
  INDEX `idx_processed_tenant`(`tenant_id` ASC, `processed_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '事件消费者幂等记录表' ROW_FORMAT = Dynamic;

CREATE TABLE `role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码，如 TENANT_ADMIN',
  `role_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色说明',
  `built_in` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否内置角色',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态：ACTIVE、DISABLED',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  `created_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间',
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '修改人',
  `version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`tenant_id` ASC, `role_code` ASC) USING BTREE,
  INDEX `idx_role_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户角色表' ROW_FORMAT = Dynamic;

CREATE TABLE `role_permission`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '角色所属租户',
  `role_id` bigint UNSIGNED NOT NULL COMMENT '角色ID',
  `permission_id` bigint UNSIGNED NOT NULL COMMENT '权限ID',
  `granted_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '授权操作人',
  `granted_at` datetime(3) NOT NULL COMMENT '授权时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_permission`(`tenant_id` ASC, `role_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `idx_role_permission_permission`(`permission_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色权限关联表' ROW_FORMAT = Dynamic;

CREATE TABLE `sla_policy`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '策略ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `policy_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '策略名称',
  `priority` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '优先级：P0、P1、P2',
  `response_minutes` int UNSIGNED NOT NULL COMMENT '响应时限分钟数',
  `resolution_minutes` int UNSIGNED NOT NULL COMMENT '解决时限分钟数',
  `enabled` tinyint UNSIGNED NOT NULL COMMENT '是否启用',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  `created_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间',
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '修改人',
  `version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sla_priority`(`tenant_id` ASC, `priority` ASC) USING BTREE,
  INDEX `idx_sla_enabled`(`tenant_id` ASC, `enabled` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户SLA策略表' ROW_FORMAT = Dynamic;

CREATE TABLE `tenant`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `tenant_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户代码，如 ACME',
  `tenant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '企业名称',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态：ACTIVE、DISABLED',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系邮箱',
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  `created_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '平台操作人',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间',
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '平台操作人',
  `version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_code`(`tenant_code` ASC) USING BTREE,
  INDEX `idx_tenant_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企业租户表' ROW_FORMAT = Dynamic;

CREATE TABLE `ticket`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `ticket_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户内展示编号',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题描述',
  `ticket_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单类型',
  `priority` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '优先级：P0、P1、P2',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前状态',
  `creator_id` bigint UNSIGNED NOT NULL COMMENT '创建人',
  `creator_department_id` bigint UNSIGNED NOT NULL COMMENT '创建时所属部门快照',
  `handling_department_id` bigint UNSIGNED NOT NULL COMMENT '当前负责部门',
  `current_assignee_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '当前处理人',
  `first_response_at` datetime(3) NULL DEFAULT NULL COMMENT '首次接单/响应时间',
  `resolution_submitted_at` datetime(3) NULL DEFAULT NULL COMMENT '提交解决方案时间',
  `resolved_at` datetime(3) NULL DEFAULT NULL COMMENT '创建人确认解决时间',
  `closed_at` datetime(3) NULL DEFAULT NULL COMMENT '关闭时间',
  `response_due_at` datetime(3) NOT NULL COMMENT '响应截止时间快照',
  `resolution_due_at` datetime(3) NOT NULL COMMENT '解决截止时间快照',
  `response_sla_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '响应SLA状态',
  `resolution_sla_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '解决SLA状态',
  `response_breached_at` datetime(3) NULL DEFAULT NULL COMMENT '响应SLA首次超时时间',
  `resolution_breached_at` datetime(3) NULL DEFAULT NULL COMMENT '解决SLA首次超时时间',
  `resolution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '当前提交的解决方案',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间',
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '最近操作人',
  `version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_no`(`tenant_id` ASC, `ticket_no` ASC) USING BTREE,
  INDEX `idx_ticket_status`(`tenant_id` ASC, `status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_ticket_creator`(`tenant_id` ASC, `creator_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ticket_assignee`(`tenant_id` ASC, `current_assignee_id` ASC, `status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_ticket_department`(`tenant_id` ASC, `handling_department_id` ASC, `status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_ticket_response_sla`(`tenant_id` ASC, `response_sla_status` ASC, `response_due_at` ASC) USING BTREE,
  INDEX `idx_ticket_resolution_sla`(`tenant_id` ASC, `resolution_sla_status` ASC, `resolution_due_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单主表' ROW_FORMAT = Dynamic;

CREATE TABLE `ticket_assignment`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分派记录ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `ticket_id` bigint UNSIGNED NOT NULL COMMENT '工单ID',
  `assignment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分派类型',
  `from_department_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '原负责部门',
  `to_department_id` bigint UNSIGNED NOT NULL COMMENT '新负责部门',
  `from_assignee_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '原处理人',
  `to_assignee_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '新处理人',
  `assigned_by` bigint UNSIGNED NOT NULL COMMENT '分派操作人',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分派或改派原因',
  `ticket_version` int UNSIGNED NOT NULL COMMENT '分派后的工单版本',
  `created_at` datetime(3) NOT NULL COMMENT '分派时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assignment_ticket`(`tenant_id` ASC, `ticket_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_assignment_assignee`(`tenant_id` ASC, `to_assignee_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单分派历史表' ROW_FORMAT = Dynamic;

CREATE TABLE `ticket_comment`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `ticket_id` bigint UNSIGNED NOT NULL COMMENT '工单ID',
  `author_id` bigint UNSIGNED NOT NULL COMMENT '评论人',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `visibility` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '可见范围：PUBLIC、INTERNAL',
  `created_at` datetime(3) NOT NULL COMMENT '评论时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_comment_ticket`(`tenant_id` ASC, `ticket_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_comment_author`(`tenant_id` ASC, `author_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单评论表' ROW_FORMAT = Dynamic;

CREATE TABLE `ticket_number_sequence`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '序列记录ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `sequence_date` date NOT NULL COMMENT '编号日期',
  `current_value` int UNSIGNED NOT NULL COMMENT '当日已使用最大序号',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间',
  `version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '并发更新版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_sequence`(`tenant_id` ASC, `sequence_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户每日工单编号序列表' ROW_FORMAT = Dynamic;

CREATE TABLE `ticket_transition`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流转记录ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `ticket_id` bigint UNSIGNED NOT NULL COMMENT '工单ID',
  `action` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `from_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原状态',
  `to_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '新状态',
  `operator_id` bigint UNSIGNED NOT NULL COMMENT '操作人',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本次操作说明',
  `ticket_version` int UNSIGNED NOT NULL COMMENT '流转后的工单版本',
  `created_at` datetime(3) NOT NULL COMMENT '流转时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_transition_ticket`(`tenant_id` ASC, `ticket_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_transition_operator`(`tenant_id` ASC, `operator_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单状态流转历史表' ROW_FORMAT = Dynamic;

CREATE TABLE `user_account`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `department_id` bigint UNSIGNED NOT NULL COMMENT '当前主部门',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录名',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 密码摘要',
  `display_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户显示名称',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态：ACTIVE、LOCKED、DISABLED',
  `last_login_at` datetime(3) NULL DEFAULT NULL COMMENT '最近登录时间',
  `password_changed_at` datetime(3) NULL DEFAULT NULL COMMENT '最近修改密码时间',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间',
  `created_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间',
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '修改人',
  `version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_username`(`tenant_id` ASC, `username` ASC) USING BTREE,
  INDEX `idx_user_department`(`tenant_id` ASC, `department_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_user_email`(`tenant_id` ASC, `email` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企业用户账号表' ROW_FORMAT = Dynamic;

CREATE TABLE `user_role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `tenant_id` bigint UNSIGNED NOT NULL COMMENT '所属租户',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id` bigint UNSIGNED NOT NULL COMMENT '角色ID',
  `assigned_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '授权操作人',
  `assigned_at` datetime(3) NOT NULL COMMENT '授权时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`tenant_id` ASC, `user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_user_role_role`(`tenant_id` ASC, `role_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

