-- V7: Add OTHER to team_name enum, extend document_type enum,
--     add download_token to documents.
-- NOTE: ALTER TYPE ADD VALUE is non-transactional in PostgreSQL — DML that
--       uses the new value must go in a later migration (V8).

-- 1. Add OTHER to team_name enum
ALTER TYPE team_name ADD VALUE IF NOT EXISTS 'OTHER';

-- 2. Add general document types to document_type enum
ALTER TYPE document_type ADD VALUE IF NOT EXISTS 'IDENTITY';
ALTER TYPE document_type ADD VALUE IF NOT EXISTS 'PROFESSIONAL_CERTIFICATE';
ALTER TYPE document_type ADD VALUE IF NOT EXISTS 'OTHER';

-- 3. Add download_token column to documents (for public presigned-style downloads)
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS download_token UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS idx_documents_download_token
    ON documents(download_token);
