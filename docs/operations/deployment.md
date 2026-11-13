# Deployment

Status: **written, never run.** These files are for the recommended hosting
option, one VPS with Docker Compose
([hosting-options.md](hosting-options.md), on `develop/hosting-options`).
Hosting hasn't been decided and no server exists yet. CI builds the backend
image on every push (`ci.yml`, job `image`). Nothing else here has run: not the
Compose stack, the scripts or the deploy workflow. Expect fixes on the first
real setup, and update this page with them.

## What runs where

```
Clients ──HTTPS──▶ api.ilecto.net (Caddy, ports 80/443)
                     │  /proho/*  →  backend:8080, with /proho stripped
                     ▼
                   backend (ghcr.io/interlecto/proho-backend:<commit>)
                     │  internal network only
                     ▼
                   postgres:17 (volume pgdata)  ──nightly, encrypted──▶ Cloudflare R2
```

| File | Does |
| ---- | ---- |
| [`backend/Dockerfile`](../../backend/Dockerfile) | Backend image. Build from the repository root, since it packages `database/migrations` |
| [`infrastructure/deployment/compose.yaml`](../../infrastructure/deployment/compose.yaml) | Caddy, backend and PostgreSQL. Only Caddy publishes ports; the database's network has no route out |
| [`Caddyfile`](../../infrastructure/deployment/Caddyfile) | TLS, the `/proho/` prefix, security headers. Hides actuator except `/proho/actuator/health` |
| [`postgres-init/10-proho-roles.sh`](../../infrastructure/deployment/postgres-init/10-proho-roles.sh) | Runs once, on an empty database: creates the owner role and `proho_app` ([tenant-isolation.md](../security/tenant-isolation.md)) |
| `env/*.env.example`, `dot-env.example` | Templates for the server's settings files |
| `bin/proho-compose` | Compose with the server's paths filled in. Use it for anything by hand |
| `bin/proho-deploy` | Deploys one master commit: backup, pull, restart, readiness check, automatic rollback |
| `bin/proho-backup`, `systemd/proho-backup.*` | Nightly `pg_dump`, encrypted with age, uploaded to R2 |
| `bin/proho-restore` | Restores a backup into a separate database, to check it |
| [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) | After CI passes on master: pushes the image to GHCR, then deploys over SSH once a server is configured |

Migrations run when the backend starts (Flyway), so a deployment is a backup
followed by a restart. With one backend instance, each deployment takes the API
down for a few seconds.

## Server layout

| Path | Holds | Owner, mode |
| ---- | ----- | ----------- |
| `/opt/proho/repo` | Clone of this repository, checked out at the deployed commit | root |
| `/opt/proho/.env` | Settings Compose needs, none of them secret (`dot-env.example`) | root, 0644 |
| `/etc/proho/proho.env` | Backend database logins (`env/proho.env.example`) | root, 0600 |
| `/etc/proho/postgres.env` | Superuser password, only until the database exists | root, 0600 |
| `/etc/proho/backup.env` | R2 token and the backup key's public half | root, 0600 |
| `/usr/local/sbin/proho-*` | Copies of `bin/` | root, 0755 |

The master copy of every secret is in Bitwarden
([secrets.md](../security/secrets.md)). Whoever creates one stores it there first.

## First setup

Assumes Ubuntu 24.04 LTS with at least 2 vCPU and 4 GB of RAM: the JVM and
PostgreSQL share it. Commands run as an administrator with `sudo`.

1. **Harden the server.** Log in with an SSH key only (`PasswordAuthentication no`
   in `sshd_config`). Turn on `unattended-upgrades`. Allow only SSH, 80, 443
   and 443/udp in `ufw`. Docker publishes ports past `ufw`, so only Caddy
   publishes any.
2. **Install** Docker Engine from Docker's apt repository (with the Compose
   plugin), plus `git`, `age` and `rclone`.
3. **Get the files.**
   ```sh
   sudo git clone https://github.com/Interlecto/proho /opt/proho/repo
   cd /opt/proho/repo/infrastructure/deployment
   sudo install -m 0755 bin/proho-* /usr/local/sbin/
   sudo install -m 0644 dot-env.example /opt/proho/.env    # then edit it
   sudo install -d -m 0700 /etc/proho
   for f in proho postgres backup; do sudo install -m 0600 env/$f.env.example /etc/proho/$f.env; done
   ```
4. **Fill in the secrets.** Generate each password with
   `openssl rand -base64 32 | tr -d '/+=' | cut -c1-32`. Store it in Bitwarden,
   then put it in `/etc/proho/*.env`. The backend refuses short, placeholder or
   shared passwords (`DatabaseCredentialsGuard`). Keep `PROHO_DB_USER=proho_app`:
   the migrations grant to that name.
5. **Backups.** In Cloudflare:
   - Create the R2 bucket `proho-backups`.
   - Add a lifecycle rule that deletes `daily/` objects after 35 days.
   - Add a bucket lock rule that keeps them at least 7 days, so a stolen token
     can't erase recent backups.
   - Create an R2 API token with "Object Read & Write" on that bucket only.

   Then create the encryption key with `age-keygen -o backup-key.txt`. Store the
   whole file in Bitwarden, put its public key (`age1…`) in `backup.env`, and
   delete the file from the server. Start the timer:
   ```sh
   sudo install -m 0644 systemd/proho-backup.* /etc/systemd/system/
   sudo systemctl daemon-reload && sudo systemctl enable --now proho-backup.timer
   ```
6. **DNS.** In Cloudflare, point `api.ilecto.net` (A, and AAAA if the server has
   IPv6) at the server. Leave it "DNS only" (grey cloud) for the first start, so
   Caddy can get its certificate directly. If you turn on the proxy later, set SSL
   to "Full (strict)".
7. **Image access.** The first master build pushes
   `ghcr.io/interlecto/proho-backend`. GHCR makes new packages private. Make it
   public in the package's settings (the source is public anyway), or run
   `docker login ghcr.io` on the server with a token that only has
   `read:packages`.
8. **First start.** Use a master commit whose Deploy workflow has pushed an
   image:
   ```sh
   sudo proho-deploy "$(git -C /opt/proho/repo rev-parse origin/master)"
   curl https://api.ilecto.net/proho/actuator/health      # {"status":"UP"}
   ```
   The database is created on this first start. Now remove the
   `POSTGRES_PASSWORD` line from `/etc/proho/postgres.env`, but keep the empty
   file: Compose requires it.
9. **Check the backups.** Run `sudo proho-backup`, then restore the result
   (below).

## Deploying from CI

`deploy.yml` deploys every master commit that passes CI, once these exist:

1. On the server, create a user that can only deploy:
   ```sh
   sudo adduser --system --group --shell /bin/sh proho-deploy
   sudo install -d -m 0700 -o proho-deploy -g proho-deploy ~proho-deploy/.ssh
   sudo install -m 0440 infrastructure/deployment/sudoers.example /etc/sudoers.d/proho-deploy
   sudo visudo -cf /etc/sudoers.d/proho-deploy
   ```
2. Create a key pair somewhere safe: `ssh-keygen -t ed25519 -N '' -C proho-ci-deploy -f proho-ci-deploy`.
   Put the public key in `~proho-deploy/.ssh/authorized_keys` in the form of
   `authorized_keys.example`. `restrict,command=` stops the key from opening a
   shell, forwarding ports or running anything except `proho-deploy`.
3. In GitHub, create the environment `production`. Give it required reviewers and
   limit its deployment branches to `master`. Add its secrets:
   - `PROHO_DEPLOY_SSH_KEY`: the private key.
   - `PROHO_DEPLOY_KNOWN_HOSTS`: the output of `ssh-keyscan -t ed25519 <host>`.
     Check its fingerprint against the provider's console, never trust the
     first answer.

   Store both in Bitwarden too, then delete the local key files.
4. Add the repository variable `PROHO_DEPLOY_HOST` (and `PROHO_DEPLOY_USER` if
   it isn't `proho-deploy`). Until it's set, the deploy job is skipped and only
   the image is pushed.

`proho-deploy` refuses any commit that isn't on `origin/master`. It backs up the
database, checks out the commit, so the Compose and Caddy files match the
image, then pulls and restarts. It waits up to two minutes for
`/actuator/health/readiness`. If the backend never gets ready, the script goes
back to the previous commit and image and fails the workflow. Migrations the
failed version applied stay applied. Flyway lets the older version start
anyway, since it ignores newer migrations.

`bin/` changes reach the server only when someone reinstalls them (step 3 of
the first setup). The running script doesn't replace itself mid-deployment.

## Operating

- **Status and logs:** `sudo proho-compose ps`, `sudo proho-compose logs -f backend`.
  Caddy logs requests as JSON, without the `Authorization` header.
- **Uptime:** point an external monitor at `https://api.ilecto.net/proho/actuator/health`.
- **Caddy and PostgreSQL updates:** `caddy:2` and `postgres:17` get minor
  releases on every deployment's pull. A new PostgreSQL major version needs a
  dump and restore, never just a tag change.
- **Rotating database passwords:** [secrets.md](../security/secrets.md#rotation),
  using `sudo proho-compose exec postgres psql -U postgres` and then
  `sudo proho-compose up -d backend`.

## Checking a backup

Do this before the pilot, then monthly.

```sh
# Copy the backup key from Bitwarden into a file only root can read, then:
sudo proho-restore proho-20261108T083000Z.dump.age /root/backup-key.txt
sudo shred -u /root/backup-key.txt
sudo proho-compose exec postgres dropdb -U postgres proho_restore_check
```

List the stored backups with `rclone lsf` against the bucket, or in the
Cloudflare dashboard. The restore lands in `proho_restore_check` and prints a
few row counts. It never touches `proho`.

## Restoring production

Only after a failed migration or data loss, and with a fresh backup of the
current state first (`sudo proho-backup`), even if that state is broken:

1. `sudo proho-compose stop backend caddy`
2. Restore into `proho_restore_check` as above, and check it.
3. Swap the databases:
   ```sh
   sudo proho-compose exec postgres psql -U postgres -c 'ALTER DATABASE proho RENAME TO proho_broken'
   sudo proho-compose exec postgres psql -U postgres -c 'ALTER DATABASE proho_restore_check RENAME TO proho'
   sudo proho-compose exec postgres psql -U postgres -c 'REVOKE ALL ON DATABASE proho FROM PUBLIC'
   sudo proho-compose exec postgres psql -U postgres -c 'GRANT CONNECT ON DATABASE proho TO proho_app'
   ```
4. Deploy the commit that matches the backup's schema:
   `sudo proho-deploy <commit>`.
5. Once it works, drop `proho_broken`. Write down what happened.

## Not covered yet

- File storage in R2 (no module stores files yet).
- Email, and any other service the backend will call.
- Contarte's API at `/contarte/` on the same host. Add a `handle_path` block in
  `Caddyfile` and its own database and roles.
- A test server separate from production. The same files work with another
  `PROHO_API_HOST`, another server and another GitHub environment.
