-- V6: Competitive Leagues & Divisions (Sprint 23)

CREATE TABLE league_seasons (
    id UUID PRIMARY KEY,
    week_key VARCHAR(20) NOT NULL UNIQUE,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_league_seasons_status ON league_seasons(status);

CREATE TABLE league_pools (
    id UUID PRIMARY KEY,
    season_id UUID NOT NULL REFERENCES league_seasons(id),
    division VARCHAR(20) NOT NULL,
    pool_number INT NOT NULL,
    participants_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_league_pools_season_div_num UNIQUE (season_id, division, pool_number)
);

CREATE INDEX idx_league_pools_season_div ON league_pools(season_id, division);

CREATE TABLE league_participants (
    id UUID PRIMARY KEY,
    pool_id UUID NOT NULL REFERENCES league_pools(id),
    season_id UUID NOT NULL REFERENCES league_seasons(id),
    user_id UUID NOT NULL REFERENCES users(id),
    division VARCHAR(20) NOT NULL,
    weekly_score BIGINT NOT NULL DEFAULT 0,
    final_rank INT,
    outcome VARCHAR(20),
    joined_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_league_participants_season_user UNIQUE (season_id, user_id)
);

CREATE INDEX idx_league_participants_pool_score ON league_participants(pool_id, weekly_score DESC, joined_at ASC);

CREATE TABLE user_league_states (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    current_division VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    highest_division VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE league_season_history (
    id UUID PRIMARY KEY,
    season_id UUID NOT NULL REFERENCES league_seasons(id),
    week_key VARCHAR(20) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    pool_id UUID NOT NULL REFERENCES league_pools(id),
    division VARCHAR(20) NOT NULL,
    next_division VARCHAR(20) NOT NULL,
    final_rank INT NOT NULL,
    weekly_score BIGINT NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_league_history_season_user UNIQUE (season_id, user_id)
);

CREATE INDEX idx_league_history_user ON league_season_history(user_id, recorded_at DESC);
