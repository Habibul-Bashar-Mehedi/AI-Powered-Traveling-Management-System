-- =============================================================================
-- Migration V008: Vendor suspension tracking + reinstatement request workflow
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. vendor: dedicated suspension fields (previously reused rejection_reason)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE vendor
    ADD COLUMN suspension_reason TEXT          NULL,
    ADD COLUMN suspended_at      DATETIME(6)   NULL,
    ADD COLUMN suspended_by      BINARY(16)    NULL,
    ADD CONSTRAINT fk_vendor_suspended_by FOREIGN KEY (suspended_by) REFERENCES users(id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. reinstatement_request
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reinstatement_request (
    request_id      BINARY(16)      NOT NULL,
    vendor_id       BINARY(16)      NOT NULL,
    message         TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL  DEFAULT 'PENDING' COMMENT 'PENDING | APPROVED | REJECTED',
    rejection_reason TEXT,
    submitted_at    DATETIME(6)     NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at     DATETIME(6),
    reviewed_by     BINARY(16),
    PRIMARY KEY (request_id),
    CONSTRAINT fk_reinstatement_vendor      FOREIGN KEY (vendor_id)    REFERENCES vendor(vendor_id),
    CONSTRAINT fk_reinstatement_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id),
    INDEX idx_reinstatement_vendor_status (vendor_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
