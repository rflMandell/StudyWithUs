-- ==============================================================================
-- StudyWithUs Database Migration: V9 Centralized System Logs
-- Reference: docs/13_ROADMAP_SPRINTS_ADMIN.md (Sprint 28)
-- ==============================================================================

CREATE TABLE system_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMPTZ NOT NULL,
    level VARCHAR(10) NOT NULL,
    module VARCHAR(80) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64),
    user_id UUID,
    http_method VARCHAR(10),
    path VARCHAR(500),
    status_code INTEGER,
    duration_ms BIGINT,
    message TEXT NOT NULL,
    context JSONB
);

CREATE INDEX idx_system_logs_timestamp_id ON system_logs(timestamp DESC, id DESC);
CREATE INDEX idx_system_logs_trace_id ON system_logs(trace_id, timestamp ASC);
CREATE INDEX idx_system_logs_level_timestamp ON system_logs(level, timestamp DESC);
CREATE INDEX idx_system_logs_module_timestamp ON system_logs(module, timestamp DESC);
CREATE INDEX idx_system_logs_user_id ON system_logs(user_id, timestamp DESC);
