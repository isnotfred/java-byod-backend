-- Migration: Remove deprecated allow_unregistered_devices setting
DELETE FROM system_settings WHERE setting_key = 'allow_unregistered_devices';
