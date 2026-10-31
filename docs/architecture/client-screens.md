# Client workflow and screens

Status: **first draft**, scoped to what the backend actually supports
today (`identity`, `communities`, `properties`, `residents` — no
`billing`, `accounting`, or `communications` yet). Revise as those
modules land; don't design screens for data the API can't yet return.

Three independently deployable clients share this workflow and the same
REST contract (`api/openapi/openapi.yaml`): web (React + TypeScript,
desktop and mobile breakpoints of the *same* app, not separate
codebases), Android (Kotlin + Jetpack Compose), and iOS (Swift + SwiftUI,
not started yet).

## 1. Shared workflow

1. **Auth.** Register (`POST /accounts`) → log in (`POST /auth/login`) →
   forgot/reset password (`POST /auth/password-reset/request` +
   `/confirm`). A new account has no community yet.
2. **Bootstrap.** `GET /communities` comes back empty for a new account.
   Prompt to **create a community** (`POST /communities` — the caller
   becomes its administrator immediately, founding membership + role
   assignment in one transaction) or wait for an invite. Invites are
   **not built yet** (no endpoint exists — same reasoning `identity`
   already used to defer its own "send an email" work), so "join an
   existing community" isn't a real path until that lands.
3. **Community switcher.** `GET /communities` → a persistent picker,
   since one account can hold several memberships (`domain-model.md` §1).
4. **Community home**, gated by the caller's role. Only `administrator`
   exists functionally today (granted automatically to whoever created
   the community) — there's no invite-and-assign-role flow yet, so every
   other assigned role (`accountant`, `treasurer`, …) is unreachable
   through the UI until invites exist.
   - **Any active member:** directory (people), units list, unit detail
     (shows its relationships).
   - **Administrator only:** create building/unit, create person, assign
     a unit relationship (owner/tenant/resident/representative — owner
     shares are capped at 100% by the API, `409 owner_share_exceeds_100`
     if exceeded).

## 2. Screens by client

**Desktop web** — full admin surface, dense tables:
- Login / Register / Forgot password
- Community switcher + "Create community" form
- Buildings: list, create
- Units: list (filter by building), create, detail (its relationships)
- People: list, create, detail
- Unit relationship assignment form, on the unit detail screen
- Account: `/me` profile, logout

**Mobile web** — the same React app as desktop, at a responsive
breakpoint, not a separate codebase. Member-first view by default
(directory, read-only units); admin create/assign forms render as
single-column stacked forms instead of table-embedded ones.

**Android** — member-first, native, same role gating as web:
- Login / Register / Forgot password
- Community switcher
- Dashboard (placeholder — no billing data exists yet to show a balance
  or statement)
- Directory (people) — read-only list
- Units — read-only list/detail
- Admin-only, trimmed: create unit, create person, assign relationship
- Profile / logout

**iOS** — not started; same screen set as Android once a project exists.

## 3. Explicitly out of scope until the backend catches up

- Invite-by-email / accept-invite screens — no endpoint yet.
- Role management UI beyond the implicit founding administrator — no
  "assign/revoke role" endpoint yet, only at community-creation time.
- Dashboard numbers, statements, payments, assessments — `billing`/
  `accounting` don't exist yet.
- Announcements/boards/messaging — `communications` doesn't exist yet.
- Offline caching behavior on mobile — nothing worth caching yet beyond
  the directory/units lists, and even those are small enough that
  caching isn't a priority before real billing data exists.

## 4. First demo slice

Enough to prove the workflow end to end, not the full screen list above:
register → log in → create a community → see it in the switcher. Web
first (`web/`, against `http://localhost:8080`), then Android
(`android/`, against `127.0.0.1:8080` via `adb reverse tcp:8080 tcp:8080`
— works for a physical device over USB and for the emulator alike).
