-- ============================================================
-- Migration: Update system settings defaults and insert new keys
-- ============================================================

-- Update existing settings
UPDATE system_settings 
SET setting_value = '5' 
WHERE setting_key = 'max_devices_per_student';

UPDATE system_settings 
SET setting_value = 'true' 
WHERE setting_key = 'allow_unregistered_devices';

-- Insert new settings
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('event_request_max_duration_days', '7', 'Maximum duration in days for an event request'),
('auto_exit_cutoff_time', '22:00', 'Cutoff time after which checked-in devices are auto-exited')
ON CONFLICT (setting_key) DO UPDATE 
SET setting_value = EXCLUDED.setting_value,
    description = EXCLUDED.description;
