CREATE TABLE auth_refresh_token_sessions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_id UUID,
    user_agent VARCHAR(512),
    ip_address VARCHAR(64)
);

CREATE UNIQUE INDEX idx_refresh_token_hash ON auth_refresh_token_sessions(token_hash);
CREATE INDEX idx_refresh_user_active ON auth_refresh_token_sessions(user_id, revoked_at, expires_at);
