-- =============================================================================
-- Migration V005: Create Vendor Dashboard Tables
-- Requirements: AITMS-BRD-VND-001 §6.1–6.4
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. vendor
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vendor (
    vendor_id           BINARY(16)      NOT NULL,
    user_id             BINARY(16)      NOT NULL,
    business_name       VARCHAR(255)    NOT NULL,
    vendor_type         VARCHAR(20)     NOT NULL COMMENT 'HOTEL | TOUR_GUIDE | TRANSPORT',
    registration_number VARCHAR(100)    UNIQUE,
    tax_id              VARCHAR(100)    UNIQUE,
    description         TEXT,
    logo_url            VARCHAR(500),
    email               VARCHAR(255)    NOT NULL,
    phone               VARCHAR(30)     NOT NULL,
    website_url         VARCHAR(500),
    address_line1       VARCHAR(255)    NOT NULL,
    address_line2       VARCHAR(255),
    city                VARCHAR(100)    NOT NULL,
    state_province      VARCHAR(100),
    country_code        CHAR(2)         NOT NULL,
    postal_code         VARCHAR(20),
    status              VARCHAR(20)     NOT NULL  DEFAULT 'PENDING_REVIEW'
                                        COMMENT 'PENDING_REVIEW | APPROVED | REJECTED | SUSPENDED',
    rejection_reason    TEXT,
    commission_rate     DECIMAL(5,2)    NOT NULL  DEFAULT 10.00,
    wallet_balance      DECIMAL(15,2)   NOT NULL  DEFAULT 0.00,
    pending_balance     DECIMAL(15,2)   NOT NULL  DEFAULT 0.00,
    payout_method       VARCHAR(20)               COMMENT 'BANK_TRANSFER | MOBILE_WALLET | PLATFORM_CREDIT',
    bank_account_info   TEXT,
    average_rating      DECIMAL(3,2),
    total_reviews       INT             NOT NULL  DEFAULT 0,
    is_email_verified   TINYINT(1)      NOT NULL  DEFAULT 0,
    created_at          DATETIME(6)     NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    approved_at         DATETIME(6),
    approved_by         BINARY(16),
    PRIMARY KEY (vendor_id),
    CONSTRAINT fk_vendor_user         FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_vendor_approved_by  FOREIGN KEY (approved_by) REFERENCES users(id),
    INDEX idx_vendor_status    (status),
    INDEX idx_vendor_user_id   (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. vendor_document
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vendor_document (
    document_id     BINARY(16)    NOT NULL,
    vendor_id       BINARY(16)    NOT NULL,
    document_type   VARCHAR(30)   NOT NULL  COMMENT 'BUSINESS_LICENSE | TAX_ID | ID_PROOF | OTHER',
    file_url        VARCHAR(500)  NOT NULL,
    file_name       VARCHAR(255)  NOT NULL,
    mime_type       VARCHAR(100)  NOT NULL,
    uploaded_at     DATETIME(6)   NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),
    verified        TINYINT(1)    NOT NULL  DEFAULT 0,
    verified_by     BINARY(16),
    PRIMARY KEY (document_id),
    CONSTRAINT fk_vdoc_vendor      FOREIGN KEY (vendor_id)   REFERENCES vendor(vendor_id)   ON DELETE CASCADE,
    CONSTRAINT fk_vdoc_verified_by FOREIGN KEY (verified_by) REFERENCES users(id),
    INDEX idx_vdoc_vendor_id (vendor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. vendor_service
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vendor_service (
    service_id              BINARY(16)      NOT NULL,
    vendor_id               BINARY(16)      NOT NULL,
    service_name            VARCHAR(255)    NOT NULL,
    service_type            VARCHAR(30)     NOT NULL  COMMENT 'HOTEL_ROOM | TOUR_PACKAGE | TRANSPORT_ROUTE',
    description             TEXT            NOT NULL,
    base_price              DECIMAL(12,2)   NOT NULL,
    currency_code           CHAR(3)         NOT NULL  DEFAULT 'USD',
    pricing_unit            VARCHAR(20)     NOT NULL  COMMENT 'PER_NIGHT | PER_PERSON | PER_SEAT | PER_TRIP',
    max_capacity            INT             NOT NULL,
    min_booking_notice      INT,
    max_booking_advance     INT,
    booking_mode            VARCHAR(10)     NOT NULL  DEFAULT 'MANUAL',
    confirmation_window     INT,
    status                  VARCHAR(10)     NOT NULL  DEFAULT 'DRAFT' COMMENT 'DRAFT | ACTIVE | INACTIVE',
    cancellation_policy     TEXT,
    location_lat            DECIMAL(10,8),
    location_lng            DECIMAL(11,8),
    location_address        VARCHAR(500),
    tags                    TEXT            COMMENT 'Comma-separated or JSON array',
    metadata                TEXT            COMMENT 'JSON — category-specific attributes',
    average_rating          DECIMAL(3,2),
    total_bookings          INT             NOT NULL  DEFAULT 0,
    is_featured             TINYINT(1)      NOT NULL  DEFAULT 0,
    created_at              DATETIME(6)     NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)     NOT NULL  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (service_id),
    CONSTRAINT fk_vsvc_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(vendor_id) ON DELETE CASCADE,
    INDEX idx_vsvc_vendor_id (vendor_id),
    INDEX idx_vsvc_status    (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. service_availability
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS service_availability (
    availability_id BINARY(16)    NOT NULL,
    service_id      BINARY(16)    NOT NULL,
    available_date  DATE          NOT NULL,
    total_slots     INT           NOT NULL,
    booked_slots    INT           NOT NULL  DEFAULT 0,
    available_slots INT           NOT NULL,
    override_price  DECIMAL(12,2),
    is_blocked      TINYINT(1)    NOT NULL  DEFAULT 0,
    PRIMARY KEY (availability_id),
    CONSTRAINT fk_avail_service FOREIGN KEY (service_id) REFERENCES vendor_service(service_id) ON DELETE CASCADE,
    INDEX idx_avail_service_date (service_id, available_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. vendor_booking
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vendor_booking (
    booking_id          BINARY(16)    NOT NULL,
    service_id          BINARY(16)    NOT NULL,
    vendor_id           BINARY(16)    NOT NULL,
    user_id             BINARY(16)    NOT NULL,
    booking_status      VARCHAR(20)   NOT NULL  DEFAULT 'PENDING'
                                      COMMENT 'PENDING | CONFIRMED | COMPLETED | CANCELLED | REJECTED',
    start_date          DATE          NOT NULL,
    end_date            DATE,
    quantity            INT           NOT NULL  DEFAULT 1,
    gross_amount        DECIMAL(12,2) NOT NULL,
    commission_amount   DECIMAL(12,2) NOT NULL,
    net_amount          DECIMAL(12,2) NOT NULL,
    payment_status      VARCHAR(25)   NOT NULL  DEFAULT 'PENDING'
                                      COMMENT 'PENDING | PAID | REFUNDED | PARTIALLY_REFUNDED',
    special_requests    TEXT,
    cancellation_reason TEXT,
    cancelled_by        VARCHAR(10)   COMMENT 'VENDOR | USER | ADMIN | SYSTEM',
    created_at          DATETIME(6)   NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at        DATETIME(6),
    completed_at        DATETIME(6),
    PRIMARY KEY (booking_id),
    CONSTRAINT fk_vbkg_service FOREIGN KEY (service_id) REFERENCES vendor_service(service_id),
    CONSTRAINT fk_vbkg_vendor  FOREIGN KEY (vendor_id)  REFERENCES vendor(vendor_id),
    CONSTRAINT fk_vbkg_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    INDEX idx_vbkg_vendor_status  (vendor_id, booking_status),
    INDEX idx_vbkg_created_at     (vendor_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. wallet_transaction
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS wallet_transaction (
    transaction_id   BINARY(16)    NOT NULL,
    vendor_id        BINARY(16)    NOT NULL,
    booking_id       BINARY(16),
    transaction_type VARCHAR(10)   NOT NULL  COMMENT 'CREDIT | DEBIT',
    amount           DECIMAL(12,2) NOT NULL,
    balance_after    DECIMAL(15,2) NOT NULL,
    description      VARCHAR(500),
    created_at       DATETIME(6)   NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (transaction_id),
    CONSTRAINT fk_wtx_vendor  FOREIGN KEY (vendor_id)   REFERENCES vendor(vendor_id),
    CONSTRAINT fk_wtx_booking FOREIGN KEY (booking_id)  REFERENCES vendor_booking(booking_id),
    INDEX idx_wtx_vendor_id (vendor_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. payout_request
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payout_request (
    payout_id       BINARY(16)    NOT NULL,
    vendor_id       BINARY(16)    NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    payout_method   VARCHAR(20)   NOT NULL  COMMENT 'BANK_TRANSFER | MOBILE_WALLET | PLATFORM_CREDIT',
    payout_details  TEXT,
    status          VARCHAR(15)   NOT NULL  DEFAULT 'PENDING'
                                  COMMENT 'PENDING | PROCESSING | COMPLETED | FAILED | CANCELLED',
    admin_note      TEXT,
    processed_by    BINARY(16),
    requested_at    DATETIME(6)   NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),
    processed_at    DATETIME(6),
    PRIMARY KEY (payout_id),
    CONSTRAINT fk_payout_vendor       FOREIGN KEY (vendor_id)    REFERENCES vendor(vendor_id),
    CONSTRAINT fk_payout_processed_by FOREIGN KEY (processed_by) REFERENCES users(id),
    INDEX idx_payout_vendor_status (vendor_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

