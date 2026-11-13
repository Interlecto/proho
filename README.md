# ProHo System

Management and social platform for the administration of communal property
and estate (*propiedad horizontal*): residential buildings, condominiums and
gated communities.

ProHo will handle billing and double-entry accounting, community
administration, and communication between residents, from web, Android and
iOS clients that share one backend. The first target market is Colombian
residential communities governed by Ley 675 de 2001, with the architecture
kept reusable for other small organizations (clubs, associations, gated
communities) afterward.

The project is a rewrite of a 2016 PHP prototype (kept in `legacy/` for
reference), built from scratch on a modern stack. See
[ROADMAP.md](ROADMAP.md) for full scope, phases and the detailed MVP
functional spec.

## Repository layout

| Path                   | Contents                                          |
| ---------------------- | ------------------------------------------------- |
| `backend/`             | Kotlin + Spring Boot API (PostgreSQL, Flyway)     |
| `web/`                 | React + TypeScript web client                     |
| `android/`             | Kotlin + Jetpack Compose client                   |
| `ios/`                 | Swift + SwiftUI client                            |
| `landing/`             | Public, static landing page (`proho.ilecto.app`)  |
| `brand/`               | Shared visual identity: wordmark, monogram, palette |
| `api/openapi/`         | Shared REST/OpenAPI contract                      |
| `database/migrations/` | Database migrations                               |
| `infrastructure/`      | Docker and deployment configuration               |
| `docs/`                | Architecture, accounting, security, API, operations |
| `legacy/`              | Original 2016 PHP implementation, kept for reference |

## Status

**Phase 1 — identity and community administration — is complete; Phase 0 —
foundation — is open only for the accountant's review and a host** (see
[ROADMAP.md](ROADMAP.md#status-at-a-glance)). Nothing is deployed yet apart
from the public landing page, so the platform can only be run locally.

- **Backend** — a Spring Boot modular monolith (`backend/`) with
  module-boundary enforcement (`ModularityTest`). Implemented modules:
  **identity** (registration, login, logout, password reset), **communities**
  (multi-community membership, invitations, roles, member profiles and
  privacy controls), **properties** and **residents** (buildings, units,
  people and their relationships, archived rather than deleted), **home**
  (the member dashboard) and **audit** (append-only events, readable by
  administrators). `billing`, `accounting`, `communications`,
  `notifications`, `payments` and `plans` are still empty module boundaries.
- **Tenant isolation** — enforced in three layers (authorization checks,
  composite foreign keys, row-level security), see
  [docs/security/tenant-isolation.md](docs/security/tenant-isolation.md).
- **Web** — sign-in, community switching, the shared navigation and member
  dashboard, and full community administration (`web/`).
- **Android and iOS** — the same navigation, dashboard and member screens,
  with trimmed administrative actions (`android/`, `ios/`). CI builds and
  tests both; the iOS app has never been run on a simulator or device, see
  [ios/README.md](ios/README.md).
- **Domain model and accounting framework** — drafted and provisional:
  [docs/architecture/domain-model.md](docs/architecture/domain-model.md) and
  [docs/accounting/](docs/accounting/README.md). Policies P1–P8 await a
  Colombian public accountant's review, which blocks Phase 2 (billing and the
  ledger).
- **Database** — Flyway migrations V1–V13 (`database/migrations/`) and a
  demo-community seed fixture.
- **API contract** — [api/openapi/openapi.yaml](api/openapi/openapi.yaml),
  covering every endpoint the backend serves.
- **Security and operations** — secrets management
  ([docs/security/secrets.md](docs/security/secrets.md), gitleaks in CI),
  privacy rules ([docs/security/privacy.md](docs/security/privacy.md), pending
  legal review), and a single-VPS deployment pipeline (Compose, Caddy,
  PostgreSQL, encrypted backups to R2;
  [docs/operations/deployment.md](docs/operations/deployment.md)) that has not
  run yet because no host has been chosen.
- **Landing page** — live at [proho.ilecto.app](https://proho.ilecto.app/)
  (`landing/`), independent of the rest of the platform.
- **Legacy salvage** — complete: [docs/legacy-salvage.md](docs/legacy-salvage.md)
  records what the 2016 design carries over. No legacy code or data is reused.

## Development

See [docs/development.md](docs/development.md) for the branching workflow and
build signing.
