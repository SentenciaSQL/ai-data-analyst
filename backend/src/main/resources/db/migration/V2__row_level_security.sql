-- The application connects as ada_app, which is not the table owner and therefore
-- obeys these policies. Login and refresh lookup run as the table owner through
-- SECURITY DEFINER functions. Row-level security is not forced, so that owner can
-- execute those functions. The API must never use the owner role.

GRANT USAGE ON SCHEMA public TO ada_app;
GRANT USAGE ON SCHEMA app TO ada_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    tenants, roles, users, refresh_tokens, audit_events
    TO ada_app;

ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenants_isolation ON tenants
    USING (id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY roles_isolation ON roles
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY users_isolation ON users
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY refresh_tokens_isolation ON refresh_tokens
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY audit_events_isolation ON audit_events
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE OR REPLACE FUNCTION app.email_exists(p_email text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1 FROM users WHERE lower(email) = lower(p_email)
    );
$$;

CREATE OR REPLACE FUNCTION app.slug_exists(p_slug text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1 FROM tenants WHERE slug = p_slug
    );
$$;

CREATE OR REPLACE FUNCTION app.find_login_user(p_email text)
RETURNS TABLE (
    user_id uuid,
    tenant_id uuid,
    email text,
    password_hash text,
    user_status text,
    tenant_status text
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT u.id, u.tenant_id, u.email::text, u.password_hash, u.status, t.status
    FROM users u
    JOIN tenants t ON t.id = u.tenant_id
    WHERE lower(u.email) = lower(p_email);
$$;

CREATE OR REPLACE FUNCTION app.find_refresh_token(p_token_hash text)
RETURNS TABLE (
    token_id uuid,
    tenant_id uuid,
    user_id uuid,
    expires_at timestamptz,
    revoked_at timestamptz
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT id, tenant_id, user_id, expires_at, revoked_at
    FROM refresh_tokens
    WHERE token_hash = p_token_hash;
$$;

REVOKE ALL ON ALL FUNCTIONS IN SCHEMA app FROM PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app TO ada_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA app REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA app GRANT EXECUTE ON FUNCTIONS TO ada_app;
