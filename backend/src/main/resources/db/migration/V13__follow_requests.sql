CREATE TABLE follow_requests (
    id UUID PRIMARY KEY,
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_follow_request_requester_target UNIQUE (requester_id, target_id),
    CONSTRAINT check_no_self_follow_request CHECK (requester_id <> target_id)
);

CREATE INDEX idx_follow_requests_target_status ON follow_requests (target_id, status);
CREATE INDEX idx_follow_requests_requester_status ON follow_requests (requester_id, status);
