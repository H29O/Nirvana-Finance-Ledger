-- =========================================================================
-- Flyway Migration: Add Email Verification System
-- Description:
--   1. Add email_verified to users table (default TRUE for existing users,
--      subsequently altered to default FALSE for all new accounts).
--   2. Create email_verifications table for secure OTP records.
-- =========================================================================

-- 1. Add email_verified column to users (existing users stay verified)
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- Set default for all subsequent user registrations to FALSE
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT FALSE;

-- 2. Create email_verifications table
CREATE TABLE IF NOT EXISTS email_verifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    used_at TIMESTAMP WITHOUT TIME ZONE,
    attempt_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_email_verifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_email_verifications_user_id ON email_verifications(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verifications_user_status ON email_verifications(user_id, status);
