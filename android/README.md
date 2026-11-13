# Android client

Kotlin + Jetpack Compose. Application ID `app.ilecto.proho`.

Scaffolded in `develop/android-shell`.

## Screens

Log in or register, then the list of communities. Opening one shows the
sections every client shares (`docs/architecture/client-screens.md`), in a
bottom bar: *Inicio* (the dashboard from `GET /communities/{id}/home`),
*Unidades* (list → detail with its relationships), *Directorio* and *Mi
perfil*. The community name in the top bar switches communities. An
administrator also gets the trimmed actions: add a unit or a person (the
add button) and add a relationship from a unit's detail. Members,
buildings, community details and history are managed on the web.

The sections are in `ui/navigation/Section.kt`, and the JSON parsing is
tested in `app/src/test/` against the shapes in `api/openapi/openapi.yaml`.
The session is still kept in memory only.
