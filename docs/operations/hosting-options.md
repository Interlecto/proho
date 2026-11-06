# Hosting options

Status: **proposal, not decided.** Written 2026-11-05. Prices and free tiers
are as understood at that date and change often: check each provider's current
pricing page before committing.

ProHo needs a server from its first release: the Kotlin/Spring Boot backend, a
PostgreSQL database, and file storage (documents, receipts, exports). This
page compares ways to host them at zero or minimal cost during development and
testing, with a path to grow once a pilot community pays for it.

The production API is planned at `https://api.ilecto.net/proho/`, a host meant
to serve several Interlecto APIs under their own path prefixes
([infrastructure.md](../infrastructure.md)). Contarte's planned server API
(its ROADMAP M9) shares ProHo's accounting contract and is expected to sit on
the same host later, as `/contarte/`. Novenario needs no database server for
now.

## What the backend requires of a host

These come from the code as it stands, and rule some options in or out.

- **A JVM container host.** Spring Boot on the JVM doesn't run on Cloudflare
  Workers, where the landing pages live. It needs a VM, a container platform,
  or serverless containers.
- **Plain PostgreSQL with row-level security.** Migrations are Flyway
  (`database/migrations/`), with `V8__tenant_isolation.sql` enabling RLS.
  Any managed Postgres works, as long as it allows creating roles.
- **Two database roles.** Flyway connects as the table owner
  (`PROHO_DB_MIGRATION_USER`); the application connects as `proho_app`
  (`PROHO_DB_USER`), which must not own the tables, be a superuser or have
  `BYPASSRLS`. `TenantIsolationGuard` refuses to start otherwise. On a managed
  service, create `proho_app` with SQL as described in `backend/README.md`,
  not through a console that may grant it the provider's admin role.
- **Direct connections, not a transaction-mode pooler.** `TenantAwareDataSource`
  sets `app.account_id` and `app.community_id` with session-level
  `set_config(..., false)` when a connection is checked out of the
  application's own pool. A transaction-mode pooler in front of the database
  (PgBouncer in transaction mode, Neon's `-pooler` endpoint, Supabase's
  transaction pooler) can hand the next statement a different server session,
  so the tenant settings wouldn't apply. Use the provider's direct endpoint;
  HikariCP already pools on the application side.
- **Configuration through environment variables.** `application.yml` reads
  the database URL and credentials from `PROHO_DB_*`, so the same image runs
  on any host.

## Options

### Local only

A developer's own PostgreSQL and `bootRun`, as `backend/README.md` describes
(Docker Compose in `infrastructure/docker/` is planned but not written yet).
Free, and where all development starts, but clients outside the developer's
machine can't reach it. Not a host for testers.

### Self-managed VM

A small VPS (Hetzner, OVH, Contabo) at about €4–6 a month, or Oracle Cloud's
Always Free ARM VM (up to 4 cores and 24 GB RAM, at no cost), running Docker
Compose:

- Caddy or nginx as the reverse proxy, with automatic TLS, routing
  `/proho/` (and later `/contarte/`) to each backend container.
- PostgreSQL in a container, one database per project.
- Nightly `pg_dump` to object storage (Cloudflare R2 or Backblaze B2).

Fixed and predictable cost, no cold starts, and it matches the shared
`api.ilecto.net` layout directly. The cost is operations: patching, backups
and restores are ours. Oracle may reclaim an idle free VM, so it suits
testing better than a pilot.

### Serverless containers with managed Postgres

- **Google Cloud Run** or **Azure Container Apps** for the backend. Both
  scale to zero and have a monthly free allowance that a test server stays
  within, so idle time costs nothing. A JVM cold start takes several seconds;
  once real users arrive, keep one instance warm (a paid minimum).
- **Neon** for PostgreSQL: a free plan with scale to zero and database
  branches (one per `develop/*` branch is useful for testing migrations).
  Use the direct endpoint, not `-pooler` (see above).

Effectively free while testing and the least operations work. Costs grow with
traffic, which is what we want, but they're less predictable than a VM.

### Azure

The managed path within one provider:

| Need | Service | Free allowance | After that |
| ---- | ------- | -------------- | ---------- |
| Backend container | Azure Container Apps (consumption plan) | Monthly grant of about 180k vCPU-seconds, 360k GiB-seconds and 2M requests; scales to zero | Pay per use |
| PostgreSQL | Azure Database for PostgreSQL – Flexible Server | B1ms, 750 hours a month and 32 GB, for the first 12 months | About $15–25 a month for B1ms plus storage; grows to General Purpose, high availability and read replicas |
| Files | Blob Storage | 5 GB for 12 months | Low storage cost; egress is charged |
| End-user sign-in (later) | Entra External ID | Roughly the first 50k monthly active users | Per user above that |

Notes:

- Don't use **Azure Spring Apps**: it is being retired and takes no new
  customers. Container Apps runs the same jar as a plain container.
- **Azure SQL Database** has a lasting free offer, but it's SQL Server, not
  PostgreSQL. Moving the migrations and RLS policies to another dialect isn't
  worth it.
- **Cosmos DB** and other non-relational stores don't fit double-entry
  accounting.
- A Visual Studio subscription includes monthly Azure credit, and Microsoft
  for Startups offers larger credits; either would cover development.
- From the second year the managed database costs several times a whole VPS.
  Container Apps with Neon is a cheaper mix that keeps the app on Azure.

### Platforms with click-to-scale plans

Render, Railway and Fly.io deploy a container from the repository with little
setup. Their free tiers have shrunk (Render's free Postgres expires after a
month), so expect a few dollars a month for anything persistent. Reasonable,
but they offer nothing the options above don't.

### Not a fit

- **Firebase / Firestore** and **Supabase's** auto-generated API: ProHo has
  its own backend, auth and relational model. Supabase is fine as plain
  managed Postgres, but its free projects pause after a week idle, and its
  pooler must be bypassed as above.
- **AWS**: new accounts get time-limited credits rather than a lasting free
  tier, and it carries the most setup. Worth revisiting only for a customer
  who requires it.

## Recommended path

1. **Now (development):** local PostgreSQL and `bootRun`, as today.
2. **Test server for testers and the clients:** one of
   - a small VPS with Docker Compose, Caddy and PostgreSQL (about €5 a
     month, always on, simplest to reason about) — preferred; or
   - Cloud Run or Azure Container Apps with Neon (free, with cold starts).

   Either way, files go to Cloudflare R2 and backups to R2 as well.
3. **Pilot community:** move the database first, since it's the hardest part
   to run well ourselves. Use managed PostgreSQL (Neon paid, Azure Flexible
   Server or the VPS provider's managed database) for automatic backups and
   point-in-time restore. Keep the backend where it is or move it to a
   container platform.
4. **Growth:** several backend instances behind a load balancer on any
   container platform. Nothing in the code has to change for this.

## Rules that keep the choice reversible

- Ship the backend as one container image, configured only by environment
  variables.
- Keep the database plain PostgreSQL. Schema changes go through Flyway; no
  provider-only extensions.
- Access files through an S3-compatible API (R2, B2, MinIO, or a thin storage
  interface of our own in front of Azure Blob Storage), never a provider SDK
  spread through the code.
- Keep DNS on Cloudflare and clients pointed at `api.ilecto.net`, so the host
  behind it can change without a client release.
- Restore a backup to a fresh database before the pilot, and on a schedule
  after it.

## Open questions

- VPS or serverless for the test server: fixed cost and no cold starts, versus
  zero cost and no operations.
- Where the database lives for the pilot, and whether Colombian data
  regulations (Ley 1581 de 2012 on personal data) favor a particular region.
  Most providers have no Colombian region; check what a pilot community's
  administrator expects.
- Whether Contarte's server shares the PostgreSQL instance (separate
  databases and roles) or gets its own.
