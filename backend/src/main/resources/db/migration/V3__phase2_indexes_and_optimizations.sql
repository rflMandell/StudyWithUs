-- ============================================================================
-- V3: Phase 2 Database Indexes & Query Optimizations
-- ============================================================================

-- Reports indexes for moderation queue filtering & performance
CREATE INDEX IF NOT EXISTS idx_reports_status_target_created ON reports (status, target_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports (reporter_id, created_at DESC);

-- Audit logs indexes for actor & target history
CREATE INDEX IF NOT EXISTS idx_audit_logs_target ON audit_logs (target_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs (actor_id, created_at DESC);

-- Subscriptions indexes for user status lookup & external provider reference
CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions (user_id, status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_external_id ON subscriptions (external_id);
CREATE INDEX IF NOT EXISTS idx_subscription_events_sub ON subscription_events (subscription_id);

-- Device tokens for push notification dispatch
CREATE INDEX IF NOT EXISTS idx_device_tokens_user_active ON device_tokens (user_id, active);
