-- Migration: Make serial_number optional in devices table
ALTER TABLE devices ALTER COLUMN serial_number DROP NOT NULL;
