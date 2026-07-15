CREATE TABLE otp_verifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    version     INT          DEFAULT 0,
    email       VARCHAR(100) NOT NULL UNIQUE,
    otp_code    VARCHAR(6)   NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    expires_at  DATETIME     NOT NULL,
    last_resend_at DATETIME NULL,
    verified_at DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_verifications_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
