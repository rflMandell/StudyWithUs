-- ==============================================================================
-- StudyWithUs Database Migration: V10 Immutable Admin Audit Logs Evolution
-- Reference: docs/13_ROADMAP_SPRINTS_ADMIN.md (Sprint 29)
-- ==============================================================================

ALTER TABLE audit_logs
    ADD COLUMN actor_role VARCHAR(30),
    ADD COLUMN reason TEXT,
    ADD COLUMN before_state JSONB,
    ADD COLUMN after_state JSONB,
    ADD COLUMN ip_address VARCHAR(64),
    ADD COLUMN user_agent VARCHAR(500),
    ADD COLUMN request_id VARCHAR(64);

CREATE INDEX idx_audit_logs_actor_created ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_action_created ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_logs_target_created ON audit_logs(target_type, target_id, created_at DESC);
