-- ============================================================
-- Migration: Add email column and pending status to users table
-- ============================================================

ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE;

ALTER TABLE users DROP CONSTRAINT chk_users_status;
ALTER TABLE users ADD CONSTRAINT chk_users_status CHECK (status IN ('active', 'inactive', 'pending'));
