ALTER TABLE user_account ADD COLUMN account_kind VARCHAR(12) NOT NULL DEFAULT 'HUMAN';

CREATE TABLE ticket_integration (
    id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    tenant_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    actor_id BIGINT UNSIGNED NOT NULL,
    priority VARCHAR(10) NOT NULL DEFAULT 'P2',
    enabled BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE ticket_integration_key (
    key_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    integration_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    secret_digest BINARY(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_ticket_integration_key FOREIGN KEY (integration_id) REFERENCES ticket_integration(id)
) ENGINE=InnoDB;

CREATE TABLE external_ticket_link (
    tenant_id BIGINT UNSIGNED NOT NULL,
    integration_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    external_ticket_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_digest BINARY(32) NOT NULL,
    ticket_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (tenant_id, integration_id, external_ticket_id),
    UNIQUE KEY uk_external_ticket_event (tenant_id, integration_id, event_id),
    UNIQUE KEY uk_external_ticket_target (ticket_id),
    CONSTRAINT fk_external_ticket_integration FOREIGN KEY (integration_id) REFERENCES ticket_integration(id),
    CONSTRAINT fk_external_ticket_target FOREIGN KEY (ticket_id) REFERENCES ticket(id)
) ENGINE=InnoDB;

INSERT INTO permission (permission_code, permission_name, resource, action, scope, description, status, created_at, updated_at)
VALUES ('ticket:claim', '领取外部工单', 'ticket', 'claim', 'DEPARTMENT', '领取当前处理部门的未指派外部工单', 'ACTIVE', NOW(3), NOW(3));
