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

## Local PostgreSQL (Windows, no admin terminal)

No local Postgres and no Docker Desktop? This gets `bootRun` working against
the defaults above, without ever needing an elevated prompt:

1. Install PostgreSQL 17 (any 14+ works): `winget install --id PostgreSQL.PostgreSQL.17`.
   It registers a Windows service (`postgresql-x64-17`, Automatic startup),
   but starting/stopping that service *does* need an elevated prompt — it's
   fine to skip that and use step 3 instead.
2. The installer sets no superuser password when run unattended, and
   `pg_hba.conf` requires one for every connection (even local), so create
   the `proho` role/database via single-user mode, which talks to the data
   directory directly and bypasses `pg_hba.conf` auth entirely. Run this once,
   with the server **stopped**:
   ```powershell
   $data = "C:\Program Files\PostgreSQL\17\data"
   $exe  = "C:\Program Files\PostgreSQL\17\bin\postgres.exe"
   @"
   ALTER USER postgres WITH PASSWORD 'postgres';
   CREATE ROLE proho LOGIN PASSWORD 'proho';
   CREATE DATABASE proho OWNER proho;
   "@ | Out-File -Encoding ascii pg_init.sql
   cmd /c "`"$exe`" --single -D `"$data`" postgres < pg_init.sql"
   ```
3. Start the server without the Windows service (works from a non-admin
   terminal; does not survive reboot/logoff — re-run as needed, or start the
   service instead if you do have an elevated prompt):
   ```powershell
   & "C:\Program Files\PostgreSQL\17\bin\pg_ctl.exe" start -D "C:\Program Files\PostgreSQL\17\data" -l pg.log
   ```
   Stop it the same way: `pg_ctl.exe stop -D "C:\Program Files\PostgreSQL\17\data"`.
4. `./gradlew bootRun` now works with no env vars — it uses `proho`/`proho`/`proho`.

None of this is needed for `./gradlew test`, which starts its own embedded
Postgres.

## Modules implemented so far

- **identity** — `POST /accounts` registers an account (email, password,
  display name). Passwords are hashed with Spring Security's delegating
  encoder (bcrypt by default); ids are UUIDv7 ([`shared/Uuid7.kt`](src/main/kotlin/net/interlecto/proho/shared/Uuid7.kt)),
  assigned by the application rather than the database, so account creation
  goes through `JdbcAggregateOperations.insert()` rather than
  `CrudRepository.save()` (Spring Data JDBC can't tell INSERT from UPDATE
  from a non-null id it didn't generate itself).
- **audit** — no API of its own. `AuditEventRecorder.record(...)` is how
  every other module writes an `AuditEvent` (see
  `docs/architecture/domain-model.md` §8); call it, don't use
  `AuditEventRepository` directly outside this module.

See [`api/openapi/openapi.yaml`](../api/openapi/openapi.yaml) for the
request/response contract.
