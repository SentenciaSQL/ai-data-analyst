#!/bin/bash
# Creates the non-owner application role used by the API.
# The official Postgres image runs this only when the data directory is empty.
set -euo pipefail

if [ -z "${APP_DB_PASSWORD:-}" ]; then
  echo "APP_DB_PASSWORD is required to create the ada_app role" >&2
  exit 1
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v app_password="$APP_DB_PASSWORD" \
  -v dbname="$POSTGRES_DB" <<'EOSQL'
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ada_app') THEN
        EXECUTE format('CREATE ROLE ada_app LOGIN PASSWORD %L', :'app_password');
    END IF;
END
$$;

GRANT CONNECT ON DATABASE :"dbname" TO ada_app;
EOSQL
