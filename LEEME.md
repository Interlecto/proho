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

En etapa temprana de la **Fase 0 — fundación** (ver [ROADMAP.md §4](ROADMAP.md)).
Todavía no hay nada desplegable ni usable; el trabajo hecho hasta ahora es la
base sobre la que se construirán las fases siguientes:

- **Modelo de dominio y marco contable** — redactados, de carácter
  provisional: [docs/architecture/domain-model.md](docs/architecture/domain-model.md)
  y [docs/accounting/](docs/accounting/README.md) (plan de cuentas, reglas de
  contabilización, invariantes del libro mayor). Pendientes de validación por
  un contador público colombiano antes de iniciar la Fase 2 (implementación
  de facturación/contabilidad).
- **Backend** — solo el esqueleto de un monolito modular en Spring Boot
  (`backend/`), con verificación de límites entre módulos (`ModularityTest`)
  ya configurada, pero sin los módulos de dominio (identidad, comunidades,
  facturación, contabilidad, …) implementados todavía.
- **Android** — esqueleto de aplicación en Jetpack Compose (`android/`), con
  firma de depuración compartida configurada, sin funcionalidades aún.
- **Web e iOS** — sin iniciar; solo README de marcador de posición.
- **Base de datos** — una migración base y un conjunto de datos de ejemplo
  (comunidad demo); todavía sin tablas de dominio.
- **Contrato de API e infraestructura** — estructura vacía (`api/openapi/`,
  `infrastructure/`), aún sin definir.
- **Aprovechamiento del legado** — completo:
  [docs/legacy-salvage.md](docs/legacy-salvage.md) registra qué se conserva
  del diseño de 2016 (pantallas, flujos), pantalla por pantalla y fase por
  fase. No se reutiliza código ni datos del legado.

## Desarrollo

Ver [docs/development.md](docs/development.md) para el flujo de trabajo con
ramas y la firma de compilaciones.
