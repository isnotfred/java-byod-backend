-- Migration: Add contact number to student registry
ALTER TABLE students ADD COLUMN contact_number VARCHAR(20);
