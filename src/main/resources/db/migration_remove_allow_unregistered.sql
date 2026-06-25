-- Migration: Remove deprecated allow_unregistered_devices and auto_exit_cutoff_time settings
DELETE FROM system_settings WHERE setting_key IN ('allow_unregistered_devices', 'auto_exit_cutoff_time');
