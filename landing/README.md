# Public landing page

Static, bilingual (ES/EN) marketing page for ProHo. No build step: deploy
`index.html` and `styles.css` as-is.

Deployment target: **https://proho.ilecto.app/** (Cloudflare Pages), with
this directory (`landing/`) as the project root. See
[docs/infrastructure.md](../docs/infrastructure.md) for the full domain plan
and [ROADMAP.md](../ROADMAP.md) for when the deployment itself is scheduled.

This page is intentionally separate from `web/` (the React/TypeScript member
and administrator app): it is public, unauthenticated, and has no
dependency on the backend.
