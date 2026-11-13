# Web client

React + TypeScript administrative and member interface, built with Vite.

| Command         | Does                                     |
| ---------------- | ----------------------------------------- |
| `npm install`    | Installs dependencies                     |
| `npm run dev`     | Runs a local dev server with hot reload   |
| `npm run build`   | Type-checks and builds to `dist/`         |
| `npm run preview` | Serves the production build locally       |

It covers sign-in and community, building, unit and people management against
a backend on `http://localhost:8080`, and has no router or state library yet.
[docs/architecture/client-screens.md](../docs/architecture/client-screens.md)
lists the screens.

`public/icon.svg` and the favicons rasterized from it come from
[`brand/`](../brand/README.md), which documents their provenance and the
color palette.
