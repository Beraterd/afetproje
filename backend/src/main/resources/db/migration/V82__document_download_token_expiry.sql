-- item 46: Document indirme linkleri artık süresiz değil. Her yetkili görüntüleme/indirme
-- isteğinde (DocumentService.issueDownloadUrl) bu alan yenilenir; serveFile süresi geçmiş
-- bir token'ı 410 GONE ile reddeder.
ALTER TABLE documents ADD COLUMN download_token_expires_at TIMESTAMPTZ;
