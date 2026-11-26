-- ==============================================================================
-- StudyWithUs Database Migration: V11 Likes on Checkins/Comments & Comment Replies
-- ==============================================================================

ALTER TABLE comments
    ADD COLUMN parent_comment_id UUID REFERENCES comments(id) ON DELETE CASCADE;

CREATE INDEX idx_comments_parent_comment_id
    ON comments (parent_comment_id)
    WHERE deleted_at IS NULL;

CREATE TABLE checkin_likes (
    checkin_id UUID NOT NULL REFERENCES checkins(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (checkin_id, user_id)
);

CREATE INDEX idx_checkin_likes_checkin_id ON checkin_likes (checkin_id);
CREATE INDEX idx_checkin_likes_user_id ON checkin_likes (user_id);

CREATE TABLE comment_likes (
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_id)
);

CREATE INDEX idx_comment_likes_comment_id ON comment_likes (comment_id);
CREATE INDEX idx_comment_likes_user_id ON comment_likes (user_id);
