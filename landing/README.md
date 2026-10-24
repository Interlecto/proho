# Public landing page

Static, bilingual (ES/EN) marketing page for ProHo. The page itself
(`public/index.html`, `public/styles.css`) has no build step.

## Deployment

Served by a Cloudflare **Worker** (not Pages), named `proho-site`, bound to
the `proho.ilecto.app` custom domain. The page is static; the worker
(`src/worker.js`) only exists to redirect any non-canonical host (e.g. a
`*.workers.dev` preview URL) to `https://proho.ilecto.app/`.

```bash
cd landing
npx wrangler deploy
```

This mirrors the setup used for [ilecto.app](https://github.com/Interlecto/ilecto)
and `novenario.ilecto.app`: one small worker per `*.ilecto.app` site, each
owning only its own custom-domain route, declared in `wrangler.jsonc`. See
[docs/infrastructure.md](../docs/infrastructure.md) for the full domain plan.

This page is intentionally separate from `web/` (the React/TypeScript member
and administrator app): it is public, unauthenticated, and has no
dependency on the backend.
