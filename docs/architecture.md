# Architecture

AI Data Analyst is a modular monolith plus one independent AI service. Spring Boot owns the SaaS. The Python engine owns language workflows. Angular is the workspace.

## Boundaries

```text
Browser
  → Angular
  → Spring Boot /api/v1
       → PostgreSQL (platform metadata, row-level security)
       → Redis (revoked access tokens)
       → AI engine /internal/v1  (X-Internal-Api-Key)
```

The browser never calls the AI engine. The engine does not manage users, tenants, subscriptions, or authentication. It trusts the tenant and user identifiers that Spring sends after the API has authorized the caller.

Customer business databases are not connected in this release. When they are, the API will open them with JDBC through a connector abstraction. The engine will still not receive customer passwords, and the platform will not store a copy of customer tables.

## Applications

### Angular (`frontend/`)

Standalone components, zoneless change detection, and signals. PrimeNG (Aura) supplies form controls, tables, and dialogs. Tailwind 4 supplies the shell. Apache ECharts renders only a validated chart specification. ngx-markdown renders analyst text with HTML sanitization enabled.

Access and refresh tokens live in `localStorage`. That choice keeps the first release free of a cookie/CSRF design. A stolen token can be replayed until logout or expiry. The API denylist and refresh rotation limit that window. A later release can move the tokens to httpOnly cookies.

Navigation:

- Overview, AI Analyst, Team, Settings, and Billing call the Phase 1 API.
- Connections, Semantic catalog, Dashboards, and Query history explain the later capability and do not show sample business data.
- Items the caller cannot use are omitted. Routes still check the permission.

### Spring Boot (`backend/`)

One deployable, package boundaries for the domains:

`identity`, `tenants`, `users`, `roles`, `audit`, `overview`, `subscriptions`, `analyst`, plus reserved packages for `databaseconnections`, `schemadiscovery`, `semanticcatalog`, `conversations`, `queries`, `analytics`, and `dashboards`.

Controllers validate input and call application services. Responses are records, not JPA entities. Errors are JSON: `timestamp`, `status`, `code`, `message`, `path`, and optional `fields`.

The analyst stream is `POST /api/v1/analyst/messages:stream`. It requires `conversations:write`. Spring copies the response bytes from the engine. The question text is not written to the API log.

### AI engine (`ai-engine/`)

FastAPI. Public routes are `/health` and `/metrics`. Everything under `/internal/v1` requires `X-Internal-Api-Key`.

The analyst workflow is a LangGraph graph: classify the question, then compose an answer. Provider calls go through one interface. `AI_PROVIDER=openai` uses LangChain's OpenAI chat model. `AI_PROVIDER=anthropic` uses the Anthropic chat model. With no API key, `configured` is false and the graph returns a fixed explanation. That explanation does not contain invented company figures.

`POST /internal/v1/sql/validate` parses one statement with SQLGlot. It accepts a select, union, intersect, or except. It rejects data changes, schema changes, multiple statements, locking reads, and a fixed set of dangerous functions. Table allow-lists are optional. Dialect names cover PostgreSQL, MySQL, SQL Server, Oracle, Snowflake, and BigQuery. Validation does not execute SQL.

## Tenancy

One PostgreSQL database and one schema for every company. Each tenant-owned row has `tenant_id`. The tenant id comes from the verified access token. Clients cannot pass a tenant id that the API uses for authorization.

The API connects as `ada_app`. That role is not the table owner and is not a superuser, so row-level security applies. A JDBC proxy sets `app.current_tenant_id` for the session when a connection is checked out and before a statement runs, then clears it when the connection closes.

Row-level security is enabled and not forced. Forced row-level security would also bind the table owner. Login has to find a user before a tenant is known, so `app.find_login_user` and the other lookup functions are `SECURITY DEFINER` and run as the owner. Forcing the policy would make those functions subject to it unless the owner were a superuser. On a managed database the owner often is not. The API must never be configured to connect as the Flyway owner.

With no tenant setting, `ada_app` sees no tenant rows. A second company cannot list the first company's users.

Email addresses are unique across the platform and stored in lowercase. Workspace slugs are unique.

## Authentication

Passwords are BCrypt hashes. Access tokens are JWT (HS256), 15 minutes, with `jti`, tenant, email, role, and permissions. Refresh tokens are opaque, stored as SHA-256 hashes, and rotated on use. Presenting a revoked refresh token revokes the rest of that user's active refresh tokens.

Logout puts the access-token `jti` on a Redis denylist until the token expires and revokes the refresh token. If Redis is unavailable, the denylist check fails open and the failure is logged. Refresh-token revocation still depends on PostgreSQL.

Unknown emails and wrong passwords return the same 401. A correct password for a disabled user or a suspended company returns 403.

Roles are created when the company registers. Permissions:

| Role | Access |
| --- | --- |
| Owner | Every permission, including billing changes |
| Admin | Every permission except billing changes |
| Analyst | Read workspace metadata, ask questions, and prepare future dashboards |
| Viewer | Read-only analyst workspace. Cannot send a question |

An owner is required to grant or change the owner role. A person cannot change their own role or disable themselves. The last active owner cannot be removed.

## Data stored here

Platform tables: `tenants`, `roles`, `users`, `refresh_tokens`, `audit_events`. The `vector` extension is installed for later semantic retrieval. No embedding rows are written yet.

Audit events share the business transaction when the action succeeds. A failed sign-in is recorded in a separate transaction so the security rollback does not drop it.

Customer schemas, credentials, semantic definitions, conversations, and query results are not stored yet. Those tables arrive with the features that own them.

## Streaming and charts

Server-Sent Events from the engine are `status`, `token`, `done`, and `error`. The workspace appends tokens into one answer and renders them as Markdown. A later `chart` event is rendered only after it matches the chart specification: bar, line, area, or pie, with finite numbers and aligned categories. The model is not allowed to send JavaScript. This release does not emit charts, because there is no query result to plot.

## Observability

The API exposes Prometheus metrics on port 9091. That port is open inside the management security chain and must stay off the public network. The engine exposes `/metrics`. Grafana has a starter dashboard. The OpenTelemetry collector receives OTLP. Application traces are not exported yet, so a stopped collector does not create noise.

## Deployment target

The three applications stay independently deployable.

- API and engine on ECS or EKS. The engine is reachable only from the API.
- Amazon RDS for PostgreSQL with pgvector. The application role stays distinct from the migration role.
- ElastiCache for Redis.
- Secrets Manager for the JWT secret, the internal API key, provider keys, and customer connection secrets. The API will encrypt connection passwords before they reach the database. This release has no connection secrets to store.
- S3 for later exports and dashboard snapshots.
- No customer database is migrated into RDS.

There is no Terraform in this repository. The Compose file is the local stand-in for that topology.

## Phase 1 and what follows

Phase 1 is the structure, Compose, authentication, tenants, users, roles, audit, the analyst stream, and the SQL validator.

Next, in order: database connections (PostgreSQL, then MySQL) with encrypted credentials and pools; schema discovery; semantic discovery with human approval and pgvector retrieval; natural-language SQL through the validator and read-only execution; charts, history, and dashboards; subscriptions, usage, more connectors, and the AWS wiring above.
