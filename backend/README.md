# Backend

Kotlin + Spring Boot API with PostgreSQL and Flyway, built as a modular
monolith with Spring Modulith. Each package under `net.interlecto.proho` is a
module (see `docs/architecture/domain-model.md`); its `package-info.java`
lists the modules it may depend on, and `ModularityTest` fails when code
crosses an undeclared boundary.

| Command                          | Does                                          |
| -------------------------------- | --------------------------------------------- |
| `./gradlew test`                 | Unit, modularity and integration tests        |
| `./gradlew bootRun`              | Runs the API on port 8080                     |
| `./gradlew bootJar`              | Builds `build/libs/proho-backend-<version>.jar` |

Integration tests start an embedded PostgreSQL, so no database or Docker is
needed to run them. `bootRun` needs a PostgreSQL server, configured with
`PROHO_DB_URL`, `PROHO_DB_USER` and `PROHO_DB_PASSWORD` (defaults:
`jdbc:postgresql://localhost:5432/proho`, `proho`, `proho`).

Migrations are in `database/migrations/` at the repository root and are
packaged into the jar.
