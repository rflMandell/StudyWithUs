-- V7: Study Analytics & Consistency Metrics (Sprint 24)

ALTER TABLE checkins ADD COLUMN IF NOT EXISTS subject VARCHAR(100);
ALTER TABLE checkins ADD COLUMN IF NOT EXISTS duration_minutes INT NOT NULL DEFAULT 0;
ALTER TABLE checkins ADD COLUMN IF NOT EXISTS tags_csv VARCHAR(500);

CREATE TABLE study_activity_records (
    checkin_id UUID PRIMARY KEY REFERENCES checkins(id),
    user_id UUID NOT NULL REFERENCES users(id),
    activity_date DATE NOT NULL,
    subject VARCHAR(100) NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE study_daily_aggregates (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    activity_date DATE NOT NULL,
    total_minutes BIGINT NOT NULL DEFAULT 0,
    sessions_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_study_daily_agg_user_date UNIQUE (user_id, activity_date)
);

CREATE INDEX idx_study_daily_agg_user_date_covering
    ON study_daily_aggregates(user_id, activity_date DESC, total_minutes, sessions_count);

CREATE TABLE study_subject_aggregates (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    subject VARCHAR(100) NOT NULL,
    total_minutes BIGINT NOT NULL DEFAULT 0,
    sessions_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_study_subject_agg_user_subject UNIQUE (user_id, subject)
);

CREATE INDEX idx_study_subject_agg_user_covering
    ON study_subject_aggregates(user_id, total_minutes DESC, sessions_count);
