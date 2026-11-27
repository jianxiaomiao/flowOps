CREATE TABLE IF NOT EXISTS email_verification_code (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    email VARCHAR(128) NOT NULL COMMENT 'Normalized lowercase email address',
    purpose VARCHAR(32) NOT NULL COMMENT 'Verification purpose, e.g. TENANT_REGISTER',
    code_hash CHAR(64) NOT NULL COMMENT 'SHA-256 hex digest of email:code',
    status VARCHAR(16) NOT NULL COMMENT 'PENDING, VERIFIED, LOCKED, REPLACED, EXPIRED',
    failed_attempts INT NOT NULL DEFAULT 0 COMMENT 'Number of failed verification attempts',
    max_attempts INT NOT NULL DEFAULT 5 COMMENT 'Maximum allowed failed attempts',
    request_ip VARCHAR(64) NOT NULL COMMENT 'Client IP address that requested the code',
    expires_at DATETIME NOT NULL COMMENT 'Expiration timestamp',
    verified_at DATETIME NULL COMMENT 'Verification completion timestamp',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    PRIMARY KEY (id),
    KEY idx_evc_email_purpose_created (email, purpose, created_at),
    KEY idx_evc_ip_created (request_ip, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Registration email verification codes';
