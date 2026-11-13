# Tenant isolation

The community is the tenant: no query, report or cache may combine records from
two communities (`docs/architecture/domain-model.md` §1). Three layers enforce
that, each independent of the others.

| Layer | Where | Catches |
| ----- | ----- | ------- |
| Authorization checks | `CommunityMembershipService`, and per-row checks in the services | Everything, with a proper 403/400 response |
| Composite foreign keys | `V8__tenant_isolation.sql`, `V9__audit_membership_community.sql` | A row referencing another community's row, whoever writes it |
| Row-level security (RLS) | `V8__tenant_isolation.sql` | A forgotten check or filter: the query finds nothing |

RLS and the foreign keys are backstops, not replacements for the checks. Without
the checks a caller gets an empty list or a database error instead of a clean
response. Foreign keys are needed as well because their checks ignore RLS.

Who can see what inside one community is a separate matter: see
[privacy.md](privacy.md).

## How a request gets its tenant

1. `BearerTokenAuthenticationFilter` authenticates the request and sets the
   account in `TenantContext`. With only an account, a request can read that
   account's memberships and active communities, and no other tenant rows.
2. `CommunityMembershipService.requireActiveMembership` checks the membership
   and only then binds the community with `TenantBinder`. The community comes
   from a passed check, never directly from the URL, so code that skips the
   check sees none of that community's rows.
3. `TenantAwareDataSource` writes the context to `app.account_id` and
   `app.community_id` on every connection it hands out, overwriting whatever a
   previous request left. `TenantBinder` also updates a connection already held
   by a running transaction.
4. The filter clears `TenantContext` when the request ends.

With nothing bound, the policies compare against NULL and match no rows, so a
missing context fails closed. That includes any future code running outside a
request, such as scheduled jobs: it has to bind a community explicitly.

## Database roles

RLS doesn't apply to superusers, `BYPASSRLS` roles or a table's owner, so two
roles are required:

| Role | Used by | Settings |
| ---- | ------- | -------- |
| Owner (`proho` locally) | Flyway | `PROHO_DB_MIGRATION_USER`, `PROHO_DB_MIGRATION_PASSWORD` |
| `proho_app` | The application | `PROHO_DB_USER`, `PROHO_DB_PASSWORD` |

`proho_app` has `SELECT`, `INSERT` and `UPDATE` on application tables, and only
`SELECT` and `INSERT` on `audit_event`, so the log is append-only for the
application. It can't delete anything or change the schema. The owner role, and
anyone with direct database access, isn't limited by any of this.

`TenantIsolationGuard` stops the application at startup if it's connected as a
role that bypasses RLS, or if any table with a `community_id` column has no RLS.
Connecting as the wrong role would otherwise disable isolation with no error.

## Adding a table

A table holding community data needs, in the same migration:

1. A `community_id uuid not null` column, even if a parent row implies it.
2. Composite foreign keys, `(parent_id, community_id)` references
   `parent (id, community_id)`, so it can't point at another community's row.
3. `alter table ... enable row level security`, and a policy like
   `unit_tenant` in `V8__tenant_isolation.sql`.
4. `grant select, insert, update on ... to proho_app`, leaving out anything the
   application doesn't need. Financial tables, which are append-only, get no
   `UPDATE`.

Skipping step 3 fails every test and every application start. Skipping step 4
fails with "permission denied" on first use.

## Limits

- **Tables outside RLS:** `account`, `account_session` and
  `password_reset_token` belong to a login rather than a community, and are read
  before anyone is authenticated.
- **`app_find_invitation`:** accepting an invitation starts with no community
  bound, so this function runs as the table's owner to get past row-level security.
  It only returns the usable invitation matching a token's hash, and can't list
  anything. Treat any other `security definer` function as a hole in isolation.
- **SQL injection:** a successful injection could call `set_config` and change
  the context. Queries are parameterized, and RLS doesn't defend against this.
- **Community keys:** the key's global uniqueness reveals whether a key is taken,
  which the `community_key_already_registered` error already does by design.
- **Connection pooling:** the context is set per connection, for the whole session
  outside a transaction. A pooler in transaction mode, such as PgBouncer's, could
  hand that session to another request, so any deployment has to pool in session
  mode or inside the application, as HikariCP does today.
- **Platform audit events:** events with no community (login, registration) are
  readable whichever community is bound. `AuditEventReader` leaves them out of a
  community's view, so read the log through it.
