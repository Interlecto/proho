# Domains and public endpoints

Status: **partially live**. The landing page is deployed; the production API
is not — see [ROADMAP.md](../ROADMAP.md) for when that's scheduled.

| Domain                        | Purpose                                              | Source                      |
| ------------------------------ | ----------------------------------------------------- | ---------------------------- |
| `https://proho.ilecto.app/`    | Public, unauthenticated landing page for ProHo        | [`landing/`](../landing/) in this repo, deployed as the `proho-site` Cloudflare Worker |
| `https://api.ilecto.net/proho/` | Production API base path for the backend              | `backend/`, once deployed    |
| `https://ilecto.app/`          | Interlecto project hub (links to ProHo, Novenario, …) | Separate project ([Interlecto/ilecto](https://github.com/Interlecto/ilecto)), not part of this repo |

Notes:

- `proho.ilecto.app` is served by a small Cloudflare **Worker** with static
  assets (not Pages), named `proho-site`, owning only that one
  custom-domain route — the same pattern `ilecto.app` and
  `novenario.ilecto.app` use. See [`landing/README.md`](../landing/README.md)
  for how to deploy it.
- `ilecto.app` is a shared hub across Interlecto projects and evolves
  independently of ProHo (other projects are added/removed over time), so it
  is intentionally not built here. ProHo's responsibility is to keep
  `proho.ilecto.app` and the `/proho/` API path correct and to link back to
  `ilecto.app` from its own landing page.
- `api.ilecto.net` is expected to host more than one Interlecto project's
  API, each under its own path prefix (`/proho/` for this one), rather than
  one subdomain per project.
- Web, Android and iOS clients will point at `https://api.ilecto.net/proho/`
  in production; local development keeps using `bootRun` on `localhost:8080`
  (see [`backend/README.md`](../backend/README.md)).
