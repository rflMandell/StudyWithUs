-- V4: Media Pipeline (Sprint 20 - Upload Seguro e Processamento de Imagens)

CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    thumbnail_key VARCHAR(500),
    original_filename VARCHAR(255) NOT NULL,
    declared_content_type VARCHAR(100) NOT NULL,
    detected_mime_type VARCHAR(100),
    declared_size_bytes BIGINT NOT NULL,
    actual_size_bytes BIGINT,
    width INT,
    height INT,
    target_id UUID,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_media_assets_owner_status ON media_assets(owner_id, status);
CREATE INDEX idx_media_assets_storage_key ON media_assets(storage_key);
CREATE INDEX idx_media_assets_purpose_target ON media_assets(purpose, target_id);
