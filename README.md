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

Early **Phase 0 — foundation** (see [ROADMAP.md §4](ROADMAP.md)). Nothing is
deployable or usable yet; the work so far is the groundwork the later phases
build on:

- **Domain model and accounting framework** — drafted and provisional:
  [docs/architecture/domain-model.md](docs/architecture/domain-model.md) and
  [docs/accounting/](docs/accounting/README.md) (chart of accounts, posting
  rules, ledger invariants). Still pending sign-off from a Colombian
  accountant before Phase 2 (billing/ledger implementation) starts.
- **Backend** — a Spring Boot modular-monolith shell (`backend/`), with
  module-boundary enforcement (`ModularityTest`) wired up but no domain
  modules (identity, communities, billing, accounting, …) implemented yet.
- **Android** — a Jetpack Compose app shell (`android/`) with shared debug
  signing configured, no features yet.
- **Web and iOS** — not started; only placeholder READMEs.
- **Database** — one baseline migration and a demo-community seed fixture;
  no domain tables yet.
- **API contract, infrastructure** — empty scaffolding (`api/openapi/`,
  `infrastructure/`), not yet defined.
- **Legacy salvage** — complete: [docs/legacy-salvage.md](docs/legacy-salvage.md)
  records what the 2016 design (screens, workflows) carries over, screen by
  screen and phase by phase. No legacy code or data is reused.

## Development

See [docs/development.md](docs/development.md) for the branching workflow and
build signing.
