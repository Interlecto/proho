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

Early **Phase 0 — foundation** (see [ROADMAP.md §4](ROADMAP.md)). The public
landing page is live; the three clients and the backend are not usable yet:

- **Domain model and accounting framework** — drafted and provisional:
  [docs/architecture/domain-model.md](docs/architecture/domain-model.md) and
  [docs/accounting/](docs/accounting/README.md) (chart of accounts, posting
  rules, ledger invariants). Still pending sign-off from a Colombian
  accountant before Phase 2 (billing/ledger implementation) starts.
- **Backend** — a Spring Boot modular monolith (`backend/`) with
  module-boundary enforcement (`ModularityTest`). One module is implemented:
  **identity** (account registration, `POST /accounts`). The rest
  (communities, billing, accounting, …) are still just declared module
  boundaries with no code.
- **Web and Android** — app shells (`web/`, `android/`) with the project's
  visual identity ([`brand/`](brand/README.md)) applied, no features yet.
- **iOS** — not started; only a placeholder README.
- **Landing page** — live at [proho.ilecto.app](https://proho.ilecto.app/)
  (`landing/`), independent of the rest of the platform.
- **Database** — two migrations (tenant baseline, identity) and a
  demo-community seed fixture.
- **API contract** — [api/openapi/openapi.yaml](api/openapi/openapi.yaml),
  started with the one endpoint the backend has (`POST /accounts`); grows
  alongside the backend.
- **Infrastructure** — empty scaffolding (`infrastructure/`), not yet defined.
- **Legacy salvage** — complete: [docs/legacy-salvage.md](docs/legacy-salvage.md)
  records what the 2016 design (screens, workflows) carries over, screen by
  screen and phase by phase. No legacy code or data is reused.

## Development

See [docs/development.md](docs/development.md) for the branching workflow and
build signing.
