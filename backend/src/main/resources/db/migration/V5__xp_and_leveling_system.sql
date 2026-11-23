-- V5: XP & Leveling System (Sprint 22 - Níveis e Progressão de Experiência)

CREATE TABLE user_levels (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    current_level INT NOT NULL DEFAULT 1,
    total_xp BIGINT NOT NULL DEFAULT 0,
    current_level_xp BIGINT NOT NULL DEFAULT 0,
    next_level_xp BIGINT NOT NULL DEFAULT 100,
    active_title VARCHAR(100) NOT NULL DEFAULT 'Calouro',
    active_avatar_border VARCHAR(100) NOT NULL DEFAULT 'BORDER_NONE',
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE xp_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    source_type VARCHAR(40) NOT NULL,
    source_id VARCHAR(120) NOT NULL,
    amount BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_xp_transactions_idempotency UNIQUE (user_id, source_type, source_id)
);

CREATE INDEX idx_xp_transactions_user_created ON xp_transactions(user_id, created_at DESC);

CREATE TABLE user_cosmetics (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    cosmetic_code VARCHAR(100) NOT NULL,
    cosmetic_type VARCHAR(30) NOT NULL,
    unlocked_at_level INT NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_cosmetics_user_code UNIQUE (user_id, cosmetic_code)
);

CREATE INDEX idx_user_cosmetics_user ON user_cosmetics(user_id);
