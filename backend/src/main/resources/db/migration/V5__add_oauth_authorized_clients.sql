CREATE TABLE IF NOT EXISTS oauth2_authorized_client (
    client_registration_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    access_token_type VARCHAR(100) NOT NULL,
    access_token_value BYTEA NOT NULL,
    access_token_issued_at TIMESTAMPTZ NOT NULL,
    access_token_expires_at TIMESTAMPTZ NOT NULL,
    access_token_scopes VARCHAR(1000),
    refresh_token_value BYTEA,
    refresh_token_issued_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (client_registration_id, principal_name)
);
