# Sistema ProHo

Plataforma de gestión y social para la administración de propiedad
horizontal: edificios residenciales, conjuntos y urbanizaciones cerradas.

ProHo manejará la facturación y la contabilidad de partida doble, la
administración de la comunidad, y la comunicación entre residentes, desde
clientes web, Android e iOS que comparten un mismo backend. El primer mercado
objetivo son las comunidades residenciales colombianas regidas por la Ley 675
de 2001, manteniendo la arquitectura reutilizable para otro tipo de
organizaciones pequeñas (clubes, asociaciones, conjuntos cerrados) más
adelante.

El proyecto es una reescritura de un prototipo en PHP de 2016 (conservado en
`legacy/` como referencia), construida desde cero sobre una base tecnológica
moderna. Ver [ROADMAP.md](ROADMAP.md) para el alcance completo, las fases y
la especificación funcional detallada del MVP.

## Estructura del repositorio

| Ruta                    | Contenido                                           |
| ---------------------- | ---------------------------------------------------- |
| `backend/`             | API en Kotlin + Spring Boot (PostgreSQL, Flyway)     |
| `web/`                 | Cliente web en React + TypeScript                    |
| `android/`             | Cliente en Kotlin + Jetpack Compose                  |
| `ios/`                 | Cliente en Swift + SwiftUI                           |
| `landing/`             | Página pública estática (`proho.ilecto.app`)         |
| `brand/`               | Identidad visual compartida: wordmark, monograma, paleta |
| `api/openapi/`         | Contrato REST/OpenAPI compartido                     |
| `database/migrations/` | Migraciones de base de datos                         |
| `infrastructure/`      | Configuración de Docker y despliegue                 |
| `docs/`                | Arquitectura, contabilidad, seguridad, API, operaciones |
| `legacy/`              | Implementación original en PHP de 2016, conservada como referencia |

## Estado de avance

**La Fase 1 — identidad y administración de comunidades — está completa; la
Fase 0 — fundación — solo queda abierta por la revisión del contador y la
elección de un hosting** (ver [ROADMAP.md](ROADMAP.md#status-at-a-glance)).
Aparte de la página pública, nada está desplegado todavía, así que la
plataforma solo puede ejecutarse localmente.

- **Backend** — monolito modular en Spring Boot (`backend/`), con
  verificación de límites entre módulos (`ModularityTest`). Módulos
  implementados: **identity** (registro, inicio y cierre de sesión,
  restablecimiento de contraseña), **communities** (pertenencia a varias
  comunidades, invitaciones, roles, perfiles de miembros y controles de
  privacidad), **properties** y **residents** (torres, unidades, personas y
  sus relaciones, que se archivan en lugar de borrarse), **home** (el
  tablero del miembro) y **audit** (eventos de solo adición, consultables
  por los administradores). `billing`, `accounting`, `communications`,
  `notifications`, `payments` y `plans` siguen siendo límites de módulo
  vacíos.
- **Aislamiento entre comunidades** — aplicado en tres capas (verificaciones
  de autorización, llaves foráneas compuestas, seguridad a nivel de fila), ver
  [docs/security/tenant-isolation.md](docs/security/tenant-isolation.md).
- **Web** — inicio de sesión, cambio de comunidad, la navegación compartida y
  el tablero del miembro, y la administración completa de la comunidad
  (`web/`).
- **Android e iOS** — la misma navegación, tablero y pantallas del miembro,
  con acciones administrativas reducidas (`android/`, `ios/`). La CI compila y
  prueba ambas; la app de iOS nunca se ha ejecutado en un simulador o
  dispositivo, ver [ios/README.md](ios/README.md).
- **Modelo de dominio y marco contable** — redactados, de carácter
  provisional: [docs/architecture/domain-model.md](docs/architecture/domain-model.md)
  y [docs/accounting/](docs/accounting/README.md). Las políticas P1–P8 esperan
  la revisión de un contador público colombiano, lo que bloquea la Fase 2
  (facturación y libro mayor).
- **Base de datos** — migraciones de Flyway V1–V13 (`database/migrations/`) y
  un conjunto de datos de ejemplo (comunidad demo).
- **Contrato de API** — [api/openapi/openapi.yaml](api/openapi/openapi.yaml),
  con todos los endpoints que expone el backend.
- **Seguridad y operación** — manejo de secretos
  ([docs/security/secrets.md](docs/security/secrets.md), gitleaks en la CI),
  reglas de privacidad ([docs/security/privacy.md](docs/security/privacy.md),
  pendientes de revisión legal) y un flujo de despliegue en un solo VPS
  (Compose, Caddy, PostgreSQL, copias de seguridad cifradas en R2;
  [docs/operations/deployment.md](docs/operations/deployment.md)) que aún no se
  ha ejecutado porque no se ha elegido un hosting.
- **Página pública** — en línea en [proho.ilecto.app](https://proho.ilecto.app/)
  (`landing/`), independiente del resto de la plataforma.
- **Aprovechamiento del legado** — completo:
  [docs/legacy-salvage.md](docs/legacy-salvage.md) registra qué se conserva
  del diseño de 2016. No se reutiliza código ni datos del legado.

## Desarrollo

Ver [docs/development.md](docs/development.md) para el flujo de trabajo con
ramas y la firma de compilaciones.
