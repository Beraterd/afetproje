-- Convert removed document types to OTHER before removing them from Java enum
UPDATE documents
SET document_type = 'OTHER'
WHERE document_type IN ('IDENTITY', 'PROFESSIONAL_CERTIFICATE');

-- Also clean up any teams that had those document types as required
UPDATE teams
SET requires_document = NULL
WHERE requires_document IN ('IDENTITY', 'PROFESSIONAL_CERTIFICATE');
