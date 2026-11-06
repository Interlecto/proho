# ProHo — notes for Claude

Management and social platform for *propiedad horizontal* (Colombian Ley 675 de
2001). Read [README.md](README.md) for the layout and status, and
[ROADMAP.md](ROADMAP.md) for scope and phases. The project is in Phase 0
(foundation); the README's Status section says which modules exist. Check it
rather than assuming, since it moves quickly.

## Where things live

- `backend/` — Kotlin + Spring Boot modular monolith (Spring Modulith). Each
  package under `net.interlecto.proho` is a module; its `package-info.java`
  declares allowed dependencies and `ModularityTest` enforces them.
- `web/` — React + TypeScript (Vite). `android/` — Kotlin + Compose.
  `ios/` — Swift + SwiftUI (placeholder only).
- `landing/` — static bilingual page served by a Cloudflare Worker
  (`proho-site`, `proho.ilecto.app`). Separate from `web/`.
- `api/openapi/openapi.yaml` — REST contract; update it with any endpoint change.
- `database/migrations/` — Flyway migrations at the repo root, packaged into
  the backend jar. `docs/accounting/` and `docs/architecture/` hold the
  provisional domain and ledger design (pending a Colombian accountant's
  sign-off before Phase 2).
- `brand/` — source of the wordmark, monogram and palette; icons in `web/`,
  `android/` and `landing/` derive from it.
- `legacy/` — 2016 PHP prototype, reference only. Do not reuse its code or data.

## Tenant isolation

The community is the tenant. No query, report or cache may mix two communities.
Enforcement has three layers (authorization checks, composite foreign keys,
row-level security), described in
[docs/security/tenant-isolation.md](docs/security/tenant-isolation.md). New
tenant-scoped tables need the same keys and RLS policy as the existing ones, and
code running outside a request (scheduled jobs) must bind a community
explicitly or it sees no rows.

## Commands

| Area | Command (run in the folder) |
| --- | --- |
| Backend | `./gradlew test`, `./gradlew bootRun`, `./gradlew bootJar` |
| Android | `./gradlew testDebugUnitTest assembleDebug` |
| Web | `npm ci`, `npm run dev`, `npm run build` |
| Landing | `npx wrangler deploy` |

Backend tests use embedded PostgreSQL, so no Docker or database is needed.
CI (`.github/workflows/ci.yml`) runs Android and backend on JDK 17 and web on
Node 24. Match those versions locally.

## Workflow and conventions

- `master` holds reviewed work that builds. Work on `develop/<topic>` branches
  cut from `master` and merge back through a pull request
  ([docs/development.md](docs/development.md)).
- Documentation is bilingual: keep `README.md` and `LEEME.md` in step.
- Debug builds use the shared keystore `android/keystore/debug.keystore`
  (application ID `net.interlecto.proho.debug`). Never commit a release key.
- Prefer small, scoped changes; this is a rewrite, so update the docs that
  describe a module when you implement it.

## Local environment (Windows 11)

- No Xcode or Mac, so `ios/` can be edited but not built or tested here. Any
  iOS build or signing work needs a macOS runner and an Apple Developer account.
- Needs JDK 17 (`JAVA_HOME`), Node 24, and the Android SDK at
  `%LOCALAPPDATA%\Android\Sdk` with `ANDROID_HOME` set.
- Not yet installed: Android `cmdline-tools`, Wrangler, Docker.
