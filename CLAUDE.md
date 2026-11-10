# ProHo — notes for Claude

Management and social platform for *propiedad horizontal* (Colombian Ley 675 de
2001). Read [README.md](README.md) for the layout and status, and
[ROADMAP.md](ROADMAP.md) for scope and phases. The project is in Phase 0
(foundation); the README's Status section says which modules exist. Check it
rather than assuming, since it moves quickly.

Rules shared by all Interlecto projects (branches, debug keystore, deployment, Google Play, visual identity, how to continue from another machine or account) are in the hub's [CLAUDE.md](https://github.com/Interlecto/ilecto/blob/master/CLAUDE.md). Where they differ, this file wins.

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

## Decisions already made (don't re-ask)

Made by the owner at the start of Phase 0 (2026-10-23) unless dated otherwise.

- **Accounting:** Colombian *propiedad horizontal* (Ley 675/2001, NIIF for small
  entities, PUC-style codes as a configurable template). An accountant has yet
  to confirm this, so flag dependent choices in the docs rather than guessing.
- **Country scope (2026-10-31):** comply with Colombian rules without being
  limited to them. Keep PUC codes, Ley 675 provisions, NIIF groups and
  withholding out of `core/accounting`; they belong in docs, templates and seed
  data. Country or sector starter packages are out of MVP scope.
- **Identity:** Spring Authorization Server inside the backend monolith (OIDC
  tokens), with Google sign-in later.
- **Hosting:** undecided. Build container images and pick a host before Phase 4.
- **Local tools:** Node LTS through winget is allowed. There is no Docker, so use
  embedded PostgreSQL locally and Testcontainers in CI.
- **Modularity:** features are modules enabled per community kind (residential
  first; clubs and associations later) and per plan.
- **Plans:** the free plan is for communities under 20 units and under 60 SMMLV
  of annual budget: functional but limited, with data pseudonymized under
  platform-held keys and limited exports. The first paid plan gives complete
  data ownership and customer-held keys. This needs legal review; the community
  stays the data controller and must always be able to get its books.
- **iOS:** built only in CI on macOS runners.

## Branches and the roadmap

- Phase 0 sub-branches are named `develop/phase0/<topic>`, because the existing
  `develop/foundation` branch blocks `develop/foundation/...`. Propose any new
  sub-branch in a plan before creating it. The owner reviews the domain model
  and accounting rules before they merge.
- **Keep `ROADMAP.md` identical on every active `develop/*` branch** (those not
  yet merged into `master`; rule set 2026-11-07). A step built on any active
  branch is marked ✅ even before it reaches `master`; say in the step's
  parenthetical which branch has it and what is unverified (for example "iOS
  never run"). When a branch changes `ROADMAP.md`, copy the same file to the
  other active branches as a ROADMAP-only commit, using a worktree so
  uncommitted work isn't disturbed. Push only branches the owner has cleared.
