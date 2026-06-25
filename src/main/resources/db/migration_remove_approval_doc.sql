-- Drop constraint and approval document columns from requests table
ALTER TABLE requests DROP CONSTRAINT IF EXISTS chk_requests_approval_doc_type;
ALTER TABLE requests DROP COLUMN IF EXISTS approval_doc_type;
ALTER TABLE requests DROP COLUMN IF EXISTS approval_doc_ref;
