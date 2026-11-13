# Infrastructure

- `docker/`: local development services (planned; `backend/README.md` covers a
  local PostgreSQL without Docker for now).
- `deployment/`: the production stack for one server: Compose with Caddy,
  the backend and PostgreSQL, scripts to deploy, back up and restore, and
  example settings files. See [docs/operations/deployment.md](../docs/operations/deployment.md).
