CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS app;

COMMENT ON SCHEMA app IS 'Privileged helpers for authentication. The API role may execute these functions and cannot bypass row-level security directly.';

CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(48) NOT NULL,
    status VARCHAR(32) NOT NULL,
    plan_code VARCHAR(64) NOT NULL DEFAULT 'trial',
    settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT tenants_slug_key UNIQUE (slug),
    CONSTRAINT tenants_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT tenants_slug_check CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$')
);

COMMENT ON TABLE tenants IS 'Customer companies. Business data stays in each customer database; this table stores only platform metadata.';

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT roles_tenant_code_key UNIQUE (tenant_id, code),
    CONSTRAINT roles_code_check CHECK (code IN ('OWNER', 'ADMIN', 'ANALYST', 'VIEWER'))
);

CREATE INDEX roles_tenant_id_idx ON roles (tenant_id);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    role_id UUID NOT NULL REFERENCES roles (id),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX users_tenant_id_idx ON users (tenant_id);

COMMENT ON COLUMN users.password_hash IS 'BCrypt hash. Plaintext passwords are never stored.';

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    user_id UUID NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    user_agent TEXT,
    ip_address VARCHAR(64),
    CONSTRAINT refresh_tokens_hash_key UNIQUE (token_hash)
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);

COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hex digest of the refresh token. The raw token is never stored.';

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    actor_user_id UUID,
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(80),
    resource_id VARCHAR(80),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX audit_events_tenant_created_idx ON audit_events (tenant_id, created_at DESC);
