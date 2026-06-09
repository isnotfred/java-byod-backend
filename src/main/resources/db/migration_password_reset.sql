-- ============================================================
-- Migration: Add password reset token columns to users table
-- ============================================================

ALTER TABLE users ADD COLUMN password_reset_token VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN password_reset_expires_at TIMESTAMPTZ;

-- Fix role column length bug to support 'super_admin' (11 characters)
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20);
