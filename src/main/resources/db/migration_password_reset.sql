-- ============================================================
-- Migration: Add password reset token columns to users table
-- ============================================================

ALTER TABLE users ADD COLUMN password_reset_token VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN password_reset_expires_at TIMESTAMPTZ;
