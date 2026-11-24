-- ==============================================================================
-- StudyWithUs Database Migration: V8 Admin Global Role & 2FA/TOTP
-- Reference: docs/13_ROADMAP_SPRINTS_ADMIN.md (Sprint 26)
-- ==============================================================================

ALTER TABLE users
    ADD COLUMN global_role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_global_role
    CHECK (global_role IN ('USER', 'SUPPORT', 'ADMIN', 'SUPER_ADMIN'));

CREATE INDEX idx_users_global_role ON users(global_role);

ALTER TABLE user_credentials
    ADD COLUMN totp_secret VARCHAR(64),
    ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN totp_verified_at TIMESTAMPTZ;
