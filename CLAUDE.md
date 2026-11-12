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

## Accounting and Contarte

The accounting module must stay compatible and exchangeable with
[Contarte](https://github.com/chlewey/Contarte) (local checkout:
`C:\Temp\Contarte`), the same author's double-entry bookkeeping app, so books
can move between the two. From the accounting point of view they are the same:
ProHo is not a general accounting app but an app that uses accounting, optimized
for its tasks. Frame differences as ProHo's domain use of a shared accounting,
not as a second accounting model.

- Contarte is upstream for the shared primitives (its ADR 0002,
  `docs/decisions/0002-proho-cross-project-accounting.md`); ProHo-specific domain
  rules stay in ProHo. If ProHo needs a primitive Contarte lacks, add it in
  Contarte first.
- Money is integer minor units plus a currency code, never floating point.
- ProHo's accounting API is the same contract as the Contarte API, for ProHo's
  own clients too, and it is centralized in the Contarte repo. Don't design a
  separate ProHo accounting API or an adapter. Billing keeps its own API.
- Any ProHo community ledger acts as an accounting server from Contarte's point
  of view. ProHo will expose a ledger API to Contarte clients through AAA
  (authentication, authorization, accounting) agreements. This is not scheduled,
  and the agreement's contents, approver and write scope are still open.
- Before working on the ledger, chart of accounts, journal entries, import/export
  or any external API, re-read Contarte's schema and status, and ProHo's
  `docs/strategy/contarte-alignment.md` (branch `develop/contarte-strategy-docs`
  until it merges).

### The contract folder, Contarte's `docs/api-contract/`

- Write any ProHo concern about the API in `proho-requirements.md`, and nowhere
  else in that folder.
- Treat every other document there as read-only, even to fix something.
  `requirements-coverage.md` is the official response.
- What is agreed there is authoritative, and ProHo can build on it. If ProHo's own
  docs differ, the folder wins. Check what is actually agreed, item by item, in
  `requirements-coverage.md`: agreeing to a requirement doesn't settle its
  wire-level shape, and the specification itself may still be an unratified draft.

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
  (application ID `app.ilecto.proho.debug`). Never commit a release key.
- Prefer small, scoped changes; this is a rewrite, so update the docs that
  describe a module when you implement it.

## Local environment (Windows 11)

- No Xcode or Mac, so `ios/` can be edited but not built or tested here. Any
  iOS build or signing work needs a macOS runner and an Apple Developer account.
- Needs JDK 17 (`JAVA_HOME`), Node 24, and the Android SDK at
  `%LOCALAPPDATA%\Android\Sdk` with `ANDROID_HOME` set.
- Not yet installed: Android `cmdline-tools`, Wrangler, Docker.
