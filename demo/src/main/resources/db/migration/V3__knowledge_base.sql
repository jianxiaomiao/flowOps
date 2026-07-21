CREATE TABLE `knowledge_category` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL,
  `parent_id` bigint UNSIGNED NULL DEFAULT NULL,
  `category_code` varchar(64) NOT NULL,
  `category_name` varchar(100) NOT NULL,
  `description` varchar(500) NULL DEFAULT NULL,
  `sort_order` int UNSIGNED NOT NULL DEFAULT 10,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(3) NOT NULL,
  `created_by` bigint UNSIGNED NULL DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL,
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL,
  `version` int UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_category_code` (`tenant_id`, `category_code`),
  KEY `idx_knowledge_category_parent` (`tenant_id`, `parent_id`, `status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户知识库分类';

CREATE TABLE `knowledge_article` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL,
  `category_id` bigint UNSIGNED NOT NULL,
  `title` varchar(200) NOT NULL,
  `summary` varchar(500) NULL DEFAULT NULL,
  `content` longtext NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `visibility` varchar(20) NOT NULL DEFAULT 'TENANT',
  `department_id` bigint UNSIGNED NULL DEFAULT NULL,
  `author_id` bigint UNSIGNED NOT NULL,
  `published_at` datetime(3) NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL,
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL,
  `version` int UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_article_visible` (`tenant_id`, `status`, `visibility`, `department_id`, `updated_at`),
  KEY `idx_knowledge_article_category` (`tenant_id`, `category_id`, `status`, `updated_at`),
  KEY `idx_knowledge_article_author` (`tenant_id`, `author_id`, `status`, `updated_at`),
  FULLTEXT KEY `ft_knowledge_article_content` (`title`, `summary`, `content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文章';

CREATE TABLE `knowledge_article_ticket` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL,
  `article_id` bigint UNSIGNED NOT NULL,
  `ticket_id` bigint UNSIGNED NOT NULL,
  `relation_type` varchar(20) NOT NULL DEFAULT 'REFERENCE',
  `created_by` bigint UNSIGNED NOT NULL,
  `created_at` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_article_ticket` (`tenant_id`, `article_id`, `ticket_id`),
  KEY `idx_knowledge_ticket` (`tenant_id`, `ticket_id`, `created_at`),
  KEY `idx_knowledge_article_link` (`tenant_id`, `article_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识文章与工单关联';

INSERT INTO permission (permission_code, permission_name, resource, action, scope, description, status, created_at, updated_at)
VALUES
  ('knowledge:read', '阅读知识库', 'knowledge', 'read', 'TENANT', '读取当前租户内可见的已发布知识', 'ACTIVE', NOW(3), NOW(3)),
  ('knowledge:write', '编写知识库', 'knowledge', 'write', 'OWN', '创建、修改和发布自己的知识草稿', 'ACTIVE', NOW(3), NOW(3)),
  ('knowledge:manage', '管理知识库', 'knowledge', 'manage', 'TENANT', '管理分类和租户内全部知识文章', 'ACTIVE', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name), resource=VALUES(resource), action=VALUES(action), scope=VALUES(scope), description=VALUES(description), status=VALUES(status), updated_at=VALUES(updated_at);

INSERT INTO role_permission (tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT r.tenant_id, r.id, p.id, NULL, NOW(3)
FROM `role` r JOIN permission p ON p.permission_code IN ('knowledge:read', 'knowledge:write', 'knowledge:manage')
WHERE r.role_code = 'TENANT_ADMIN' AND r.status = 'ACTIVE'
ON DUPLICATE KEY UPDATE granted_at = role_permission.granted_at;
