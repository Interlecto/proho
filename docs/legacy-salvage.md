# What the new platform takes from legacy ProHo

The 2016 PHP implementation lives in `legacy/`. Its code is not reused: the
custom ORM, database drivers and page rendering are replaced by the new
stack, and its password hashing (plain text, MD5, unsalted SHA-256) must not
be carried over in any form. The SQLite database `legacy/dist/data/proho.db`
is empty and the MySQL install script holds only reference tables, so there
is no data to migrate.

What the legacy project does provide is product design. This document records
it so the new clients and backend can build on it.

## Screen map

From the mockups in `legacy/maqueta/`. The signed-in navigation is the same on
every page.

| Mockup               | Spanish label        | New platform                                  | Phase |
| -------------------- | -------------------- | --------------------------------------------- | ----- |
| `escritorio.html`    | Escritorio           | Member dashboard                              | 1     |
| `cuentas.html`       | Mis cuentas          | Member statement: balance, pay, report payment | 2    |
| `pagar.html`         | Pago en línea        | Electronic payment                            | 5     |
| `contable.html`      | Contabilidad         | Accounting reports, filtered by role          | 2     |
| `apartamento.html`   | Mi apartamento       | Unit with owners, residents and household     | 1     |
| `vecinos.html`       | Mis vecinos          | Community activity feed                       | 3     |
| `noticias.html`      | Noticias             | Official announcements                        | 3     |
| `blog.html`          | Blog                 | Discussion boards                             | 3     |
| `tienda.html`        | Tienda               | Marketplace (backlog)                         | —     |
| `perfil.html`        | Perfil               | Profile and privacy                           | 1     |
| `configuracion.html` | Configuración        | Profile, privacy and notification settings    | 1     |
| `ayuda.html`         | Ayuda                | Help                                          | 4     |
| `buscar.html`        | Búsqueda             | Search (backlog)                              | —     |
| `index.html`         | Entrada              | Sign-in, with public teaser                   | 1     |
| `registro.html`      | Registro de usuario  | Registration                                  | 1     |
| `recuperar.html`     | Recuperar contraseña | Account recovery                              | 1     |
| `visitantes.html`    | —                    | Public visitor page (backlog)                 | —     |

### The dashboard as first specification

`escritorio.html` shows the member's state first, then the news:

> **Tu estado** — Está pendiente tu pago de administración de abril por
> $450.000, a vencerse el 30 de abril. [Pagar en línea] [Reportar pago]
>
> **Noticias** — Cuota extraordinaria: la asamblea aprobó una cuota
> extraordinaria…

The dashboard on all three clients should keep that order: what the member
owes and when, the two payment actions, then recent announcements.

### Resident-reported payments

"Reportar pago" is how most Colombian communities are paid: the resident
transfers money to the community's bank account and tells the administration.
The new platform models it as a report with amount, date, method, reference
and an optional receipt image, which stays pending until a treasurer confirms
or rejects it. Only confirmation records the payment in the ledger. See
ROADMAP §3.2.

## Vocabulary

Terms taken from the mockups for the Spanish UI and for domain names in code.

| Spanish                           | Meaning                                    | Code term                  |
| --------------------------------- | ------------------------------------------ | -------------------------- |
| Propiedad horizontal              | Condominium regime                         | community                  |
| Cuota (de administración)         | Ordinary monthly assessment                | ordinary assessment        |
| Cuota extraordinaria              | Special assessment                         | extraordinary assessment   |
| Coeficiente de copropiedad        | Unit's ownership share, used to split fees | coefficient                |
| Asamblea                          | Owners' general meeting                    | assembly                   |
| Administración                    | Building management (also an author name)  | administration             |
| Reportar pago                     | Report a payment made outside the platform | payment report             |
| Arriendo / canon de arriendo      | Rent / monthly rent                        | rent                       |
| "Jairo, del 509"                  | Residents are named by their unit          | display name + unit        |

The mockups split an extraordinary assessment "por el coeficiente de
copropiedad", so coefficient allocation is required from the first
assessment module.

## Publishing workflow

From `legacy/dist/install/mysql.sql` (`news_status`, `blog_status`,
`news_item`, `news_history`). Adopted in ROADMAP §3.5.

| Legacy state    | Meaning                                  | New state                      |
| --------------- | ---------------------------------------- | ------------------------------ |
| `draft`         | Borrador                                 | draft                          |
| `pending`       | Pendiente de aprobación                  | pending approval               |
| `pending_pub`   | Pendiente de publicación                 | scheduled                      |
| `private`       | Publicado privadamente                   | published, audience community  |
| `privileged`    | Publicado privilegiadamente              | published, audience privileged |
| `public`        | Publicado públicamente                   | published, audience public     |
| `un_*`          | Removido tras publicado                  | withdrawn, keeping its audience |

The legacy design merged state and audience into one value. The new model
keeps them as separate fields, which removes the three `un_*` states.

Also kept: `publish_on` / `publish_off` scheduling, edit history with editor
and diff, categories, tags, Markdown bodies (the `formats` table), and several
URL slugs per post.

## People and permissions

- **Person is not account.** `mod/pro` defines a `person` and `mod/sec` turns
  a person into a `user` later (`db_user::upgrade`). The new model keeps that:
  residents, owners and household members exist without logging in, and an
  account can be linked when they register.
- **Visibility tiers.** The legacy permission levels 0–9 combined an action
  (read, edit, create) with a tier (basic, privileged, all). Roles replace the
  numeric levels, but the three tiers remain as a classification of data and
  posts: community, privileged (board and administration), and public.

## Backlog

Modules planned in 2016 that are not in the MVP. Listed in ROADMAP §3.6.

| Legacy module | Purpose                                         |
| ------------- | ----------------------------------------------- |
| `mkt`         | Marketplace: units for rent or sale, services   |
| `gal`         | Photo galleries                                 |
| `doc`         | Documents (rules, minutes, official documents)  |
| `src`         | Search                                          |
| `rem`         | Threaded comments                               |
| `dsh`         | Public and protected dashboards (visitor page)  |

## Demo data

The legacy sample data (one building, residents named by unit, a matching
blog, store and news) is the basis of `database/seed/demo-community.json`,
rewritten with invented people.

## Not carried over

- All PHP code: `db_obj` ORM, MySQL/SQLite drivers, page and API rendering.
- Password hashing in `mod/sec/db.php`, which is insecure.
- FrontAccounting and Parsedown, which were never in the repository; the
  double-entry ledger in ROADMAP §3.2 replaces the former.
- Empty modules (`acc`, `msg`, `mkt` contain only stubs), plugin and skin
  folders, and the HTTP status table.
