# ProHo System

Management and social platform for the administration of communal property
and estate (*propiedad horizontal*): residential buildings, condominiums and
gated communities.

ProHo handles billing and double-entry accounting, community administration,
and communication between residents, from web, Android and iOS clients that
share one backend. See [ROADMAP.md](ROADMAP.md) for scope and phases.

## Repository layout

| Path                   | Contents                                          |
| ---------------------- | ------------------------------------------------- |
| `backend/`             | Kotlin + Spring Boot API (PostgreSQL, Flyway)     |
| `web/`                 | React + TypeScript web client                     |
| `android/`             | Kotlin + Jetpack Compose client                   |
| `ios/`                 | Swift + SwiftUI client                            |
| `landing/`             | Public, static landing page (`proho.ilecto.app`)  |
| `api/openapi/`         | Shared REST/OpenAPI contract                      |
| `database/migrations/` | Database migrations                               |
| `infrastructure/`      | Docker and deployment configuration               |
| `docs/`                | Architecture, accounting, security, API, operations |
| `legacy/`              | Original 2016 PHP implementation, kept for reference |

See [docs/infrastructure.md](docs/infrastructure.md) for the planned public
domains (`proho.ilecto.app`, `api.ilecto.net/proho/`).

## Development

See [docs/development.md](docs/development.md) for the branching workflow and
build signing.
