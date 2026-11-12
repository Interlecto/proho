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
   assignment in one transaction) or open an invitation link, which
   previews the community and joins it (`POST /invitations/preview`,
   `/accept`). There's no email yet, so an administrator shares the link.
3. **Community switcher.** `GET /communities` → a persistent picker,
   since one account can hold several memberships (`domain-model.md` §1).
4. **Community home**, gated by the caller's role. Only `administrator`
   changes what the screens offer so far, and privileged roles see more
   people's details (`docs/security/privacy.md`).
   `GET /communities/{id}/membership` returns the caller's roles.
   - **Any active member:** units list and unit detail; a directory of the
     people who chose to share their name, and of people on their own units,
     limited as in `docs/security/privacy.md`; and their own account and
     profile, where they choose what to share.
   - **Administrator only:** create, edit, archive and restore buildings,
     units and people; see archived records; edit the community's details;
     add a unit relationship (owner shares can't pass 100% on any day,
     `409 owner_share_exceeds_100`) and end one; invite members, assign and
     end their roles, link them to a person, suspend and remove them.
5. **Nothing is deleted.** Archiving hides a record from the normal lists
   and stops it being linked to. It's blocked while the record is in use
   (`building_in_use`, `unit_in_use`, `person_in_use`): archive a building's
   units first, and end a unit's or person's relationships first. Ended
   relationships stay as history.

## 2. Screens by client

**Desktop web** — full admin surface, dense tables:
- Login / Register / Forgot password
- Community switcher + "Create community" form
- Community details: view, edit
- Buildings: list, create, edit, archive, restore
- Units: list (filter by building), create, edit, archive, restore, detail
  (its relationships)
- People: list, create, edit, archive, restore, detail (their relationships)
- Add and end unit relationships, on the unit detail screen
- Members: list, roles, link to a person, suspend, reactivate, remove
- Invitations: create and share a link, list, revoke; accept from the link
- My profile: account name and language, contact details, sharing choices,
  leave the community
- Logout

All of the above is built, in `web/src/CommunityScreen.tsx`.

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

**iOS** — same screen set as Android. A skeleton with the first demo
slice exists (`ios/`), not yet verified on a Mac; plan in `ios/README.md`.

## 3. Explicitly out of scope until the backend catches up

- Sending invitations by email — no notifications module yet.
- Dashboard numbers, statements, payments, assessments — `billing`/
  `accounting` don't exist yet.
- Announcements/boards/messaging — `communications` doesn't exist yet.
- Offline caching behavior on mobile — nothing worth caching yet beyond
  the directory/units lists, and even those are small enough that
  caching isn't a priority before real billing data exists.

## 4. First demo slice

The first slice proved the workflow end to end: register → log in → create
a community → see it in the switcher. That's still all Android has. Web
first (`web/`, against `http://localhost:8080`), then Android
(`android/`, against `127.0.0.1:8080` via `adb reverse tcp:8080 tcp:8080`
— works for a physical device over USB and for the emulator alike).
