CREATE TABLE `knowledge_recommendation_feedback` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL,
  `ticket_id` bigint UNSIGNED NOT NULL,
  `article_id` bigint UNSIGNED NOT NULL,
  `status` varchar(20) NOT NULL COMMENT 'RECOMMENDED, ADOPTED, REJECTED',
  `similarity` double NULL DEFAULT NULL,
  `created_by` bigint UNSIGNED NOT NULL,
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_feedback` (`tenant_id`, `ticket_id`, `article_id`),
  KEY `idx_knowledge_feedback_status` (`tenant_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库推荐反馈表';
