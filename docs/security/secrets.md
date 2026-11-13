# Secrets management

Status: **defined, partly provisional.** The rules, the inventory and the checks
below apply now. Where production secrets are stored depends on the hosting
decision ([operations/hosting-options.md](../operations/hosting-options.md), on
`develop/hosting-options`), so that part is written for the recommended option,
a VPS, and gets revised when hosting is decided.

A secret is anything that grants access or proves identity: passwords, private
keys, API keys and tokens, signing keys. Some values in this repository look like
secrets but are public on purpose. They are listed below too, so nobody mistakes
one for the other.

## Rules

1. **No secret in the repository**, in any branch or commit. A secret that was
   committed is leaked even after it's deleted, because it stays in the
   history. The only fix is to rotate it (see [If a secret leaks](#if-a-secret-leaks)).
2. **Configuration, not code.** The backend reads every secret from an
   environment variable (`PROHO_*`). The same image runs everywhere; only the
   environment differs.
3. **One secret per purpose and per environment.** Development, test servers
   and production never share a password. The application and migration
   database roles never share one either.
4. **Generated, never invented.** At least 32 random characters, for example
   `openssl rand -base64 32`. The backend refuses anything shorter than 16
   (see [Checks](#checks)).
5. **Least privilege.** Each secret grants only what its user needs: the
   application's database role can't change the schema, delete rows or bypass
   row-level security ([tenant-isolation.md](tenant-isolation.md)), and a CI
   deploy key can only deploy.
6. **One master copy.** Every production secret's master copy lives in
   Interlecto's password manager, Bitwarden ([below](#password-manager)).
   Servers and CI hold copies. Whoever creates or rotates a secret updates the
   master copy first.
7. **Never logged.** Secrets don't go into logs, error messages, audit events
   or support tickets. Error messages name the variable, never the value.
8. **Every new secret gets a row** in the inventory below, in the same pull
   request that starts using it.

## Inventory

### In use today

| Secret | Used by | Local development | CI | Production |
| ------ | ------- | ----------------- | -- | ---------- |
| `PROHO_DB_PASSWORD`, role `proho_app` | Backend at runtime | Default `proho_app` from `application-dev.yml` | Not needed: tests start an embedded PostgreSQL with throwaway logins | Environment file on the host (below) |
| `PROHO_DB_MIGRATION_PASSWORD`, the table owner | Flyway at startup | Default `proho` from `application-dev.yml` | Not needed | Environment file on the host |
| PostgreSQL superuser | Creating roles and databases by hand, never the application | Set when installing PostgreSQL | Not needed | Password manager. With the containerized database, also `/etc/proho/postgres.env` until the database is created, then removed. Never in the backend's environment |

The backend has no other secrets today. Sessions and password resets (and
invitations) use random tokens that are
stored hashed, not signed with a key, so there is no signing key to protect. A token is still sensitive while it's
valid. The `dev` profile logs password-reset tokens because there's no email
yet, which is one reason it must never be active on a real deployment.

### Public on purpose

| Value | Why it isn't secret |
| ----- | ------------------- |
| `android/keystore/debug.keystore` and its passwords `android` | Shared debug key, so builds from any machine update the app on test devices ([development.md](../development.md)). It signs debug builds only; the stores reject them. |
| The `dev` defaults in `application-dev.yml` | Logins that exist only on a developer's own PostgreSQL. The backend refuses them anywhere else. |
| Test fixtures (`backend/src/test/`, `ios/ProHoKit/Tests/`) | Fake values against throwaway servers or stubs. |

### Expected later

Add the row for real when the secret starts being used.

| Secret | When | Where it will live |
| ------ | ---- | ------------------ |
| Android upload key (Play App Signing) and its passwords | First Play Store build | Password manager; CI secrets as base64 for release builds. Google holds the app signing key. |
| App Store Connect API key (`.p8`) and distribution certificate | First TestFlight build | Password manager and CI secrets, or Xcode Cloud |
| Deploy key (SSH) for the server, `PROHO_DEPLOY_SSH_KEY`, and the server's host key, `PROHO_DEPLOY_KNOWN_HOSTS` | First deployment (`.github/workflows/deploy.yml`) | Password manager; CI secret in the `production` environment, which needs approval. The server only lets the key run `proho-deploy` ([deployment.md](../operations/deployment.md)) |
| Object storage keys (Cloudflare R2) for files | File storage | Host environment file |
| R2 token for backups (`R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`) | Nightly backups (`proho-backup`) | `/etc/proho/backup.env`, master copy in the password manager. R2 has no write-only token, so it's "Object Read & Write" on the backup bucket only, and the bucket's lock rule keeps recent backups from being deleted |
| Backup encryption key (age), private half | Restoring a backup | Password manager only, never on the server except while restoring. The public half, in `backup.env`, isn't secret |
| Cloudflare API token | If the landing page is deployed from CI rather than by hand | CI secret, scoped to the one Worker |
| Email provider API key | Notifications module | Host environment file |
| Payment provider keys and webhook signing secrets | Payments (Phase 2/3) | Host environment file. The most sensitive in this list; see rotation below |
| Key for encrypting personal data | When encryption of personal data is built ([privacy.md](privacy.md)) | Kept apart from database backups, or a backup holds both the data and its key |

## Password manager

Decided 2026-11-07: **Bitwarden, the cloud service, for now**, in an
organization account for Interlecto. It's open source and needs no operations
from us. Its data can move to a self-hosted Bitwarden or to Vaultwarden later,
with the same apps, if the project decides to run its own.

- **What goes in:** the master copy of every secret marked "Password manager"
  in the inventory, and of every production and CI secret. Not development
  defaults or test fixtures.
- **Organization, not personal vaults.** Project secrets live in the
  organization's collections, so access survives someone leaving and can be
  granted per collection: for example one for production, one for app-store
  signing. Store each item under the inventory's name for it, such as
  `PROHO_DB_PASSWORD (production)`.
- **Two-step login is required** for every member, and the account's recovery
  code is kept offline, apart from the vault.
- **Least access.** Give members only the collections they need. When someone
  leaves, remove them from the organization and rotate what they could see
  (see [Rotation](#rotation)).
- **Export now and then.** An encrypted export, kept offline, covers losing
  access to the service.
- **Machines don't read it yet.** CI uses GitHub Actions secrets and the server
  its environment file, both copied by hand from the vault. Once there's more
  than one server, Bitwarden Secrets Manager can hand secrets to machines
  directly.

## Where secrets live

- **Local development:** the `dev` defaults, or your own values in environment
  variables or an untracked `application-local.yml`. `.gitignore` already
  excludes `.env`, `*.env`, `application-local.*`, keystores, private keys and
  Apple signing files.
- **CI:** GitHub Actions encrypted secrets. Deployment secrets go in a
  `production` environment with required reviewers, so a workflow on a branch
  can't read them. Pull requests from forks get no secrets.
- **Production (provisional, for the VPS option):** one environment file per
  service, such as `/etc/proho/proho.env`, owned by root with mode `0600`, passed
  to the container with Docker Compose's `env_file`
  ([deployment.md](../operations/deployment.md)). Never in the image, the
  Compose file or the repository. On a managed platform, its secret store
  replaces the file.

## Checks

- **CI scans the whole history** with [gitleaks](https://github.com/gitleaks/gitleaks)
  (`.github/workflows/ci.yml`, job `secrets`, configured by `.gitleaks.toml`).
  A finding fails the build. If it's a real secret, rotate it. Allowlist only
  values that are fake or public on purpose, with a comment saying why.
- **The backend refuses development database passwords**
  (`DatabaseCredentialsGuard`). Before Flyway connects, it stops the
  application if either password is empty, shorter than 16 characters, equal
  to its role's name or a placeholder, or if the two roles share a password or
  are the same role. Only the `dev` profile and the tests opt out, through
  `proho.security.allow-development-credentials=true`. Never set it on a real
  deployment.
- **`TenantIsolationGuard`** separately refuses an application role that could
  bypass row-level security ([tenant-isolation.md](tenant-isolation.md)).

## Rotation

Rotate a secret when someone who knew it leaves, when it may have leaked, and
otherwise once a year. Payment and deploy secrets: also whenever their
provider recommends it.

Database passwords, with a short restart and no data change:

1. Generate a new password and update the master copy.
2. `ALTER ROLE proho_app PASSWORD '<new password>';` (or the owner's role),
   as the superuser. PostgreSQL stores it as a SCRAM hash.
3. Update the environment file, then restart the backend.
4. Check that the backend is healthy (`/actuator/health`) and that the old
   password no longer connects.

## If a secret leaks

1. **Rotate it first.** Deleting it from the repository or the log doesn't
   undo the leak.
2. Check what it allowed, and look for use since the leak: database logs, the
   provider's access logs, the audit log.
3. Remove it from wherever it leaked. If that's the git history, rewriting
   history is optional cleanup after the rotation, not a fix.
4. Write down what happened and what changed, so the same path can be closed.
