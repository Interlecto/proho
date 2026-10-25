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

## Modules implemented so far

- **identity** — `POST /accounts` registers an account (email, password,
  display name). Passwords are hashed with Spring Security's delegating
  encoder (bcrypt by default); ids are UUIDv7 ([`shared/Uuid7.kt`](src/main/kotlin/net/interlecto/proho/shared/Uuid7.kt)),
  assigned by the application rather than the database, so account creation
  goes through `JdbcAggregateOperations.insert()` rather than
  `CrudRepository.save()` (Spring Data JDBC can't tell INSERT from UPDATE
  from a non-null id it didn't generate itself).

See [`api/openapi/openapi.yaml`](../api/openapi/openapi.yaml) for the
request/response contract.
