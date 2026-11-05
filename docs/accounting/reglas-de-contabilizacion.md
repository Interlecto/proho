# Reglas de contabilización

*[Read in English](posting-rules.md)*

Cómo se convierte cada evento financiero en un asiento contable. Las
cuentas se escriben como roles (ver
[plan-de-cuentas.md](plan-de-cuentas.md)); los códigos en los ejemplos
son los valores predeterminados de la plantilla. Las claves del
subsistema auxiliar por cuenta de cobro se muestran entre corchetes:
`receivable.ordinary [509]`.

Principios generales:

- **Un documento fuente, un asiento.** Cada cargo, pago, confirmación de
  reporte, factura de proveedor o desembolso produce exactamente un
  asiento, contabilizado en la misma transacción de base de datos que el
  documento.
- **Las correcciones nunca editan.** Toda corrección es una reversión
  (regla R13), opcionalmente seguida de un nuevo asiento correcto.
- **Las cuentas por cobrar solo se mueven a través de la facturación**
  (invariante del libro mayor L8).
- **Los montos** están en COP sin decimales en los ejemplos.

## Reglas

### R1 — Emitir una cuota ordinaria

Un asiento `CA` para toda la corrida, con una línea de cuenta por cobrar
por cada cuenta de cobro.

| Débito                                 | Crédito              |
| ---------------------------------------- | ---------------------- |
| `receivable.ordinary [cada cuenta]`      | `income.ordinary`      |

Los Robles, abril: 3.000.000 repartidos por coeficiente →
105: 432.000 · 212: 390.000 · 307: 480.000 · 408: 432.000 · 509: 570.000 ·
702: 696.000.

### R2 — Emitir una cuota extraordinaria

Política P2 predeterminada (fondo restringido):

| Débito                                        | Crédito                 |
| ------------------------------------------------ | -------------------------- |
| `receivable.extraordinary [cada cuenta]`         | `fund.extraordinary`       |

Cuando se ejecutan las obras aprobadas (R11), el fondo se libera a
ingreso por el monto ejecutado:

| Débito                 | Crédito                    |
| ------------------------ | ----------------------------- |
| `fund.extraordinary`     | `income.extraordinary`        |

Con la política alternativa (ingreso al emitirse), el crédito en el
primer asiento es `income.extraordinary` y no hay liberación posterior.

Cuotas en instalamentos: un cargo, y un asiento, por cada instalamento,
en su fecha de emisión.

### R3 — Recibir un pago (registrado por el personal)

Un asiento `RC` construido a partir de las aplicaciones del pago:

| Débito                                | Crédito                                                      |
| ---------------------------------------- | ---------------------------------------------------------------- |
| `cash.box` o `cash.bank [banco]`         | una línea por cada rol de cuenta por cobrar aplicado `[cuenta]`  |
|                                           | `credit.owner [cuenta]` por cualquier remanente sin aplicar      |

Ejemplo: 509 paga su cuota de abril por transferencia, 570.000 →
Db `cash.bank` 570.000 / Cr `receivable.ordinary [509]` 570.000.

### R4 — Pago reportado por el residente

- `submitted` (enviado): sin asiento.
- `rejected` (rechazado): sin asiento; el reporte se cierra con un motivo
  que el residente puede ver.
- `confirmed` (confirmado): exactamente R3, con fecha la **fecha de pago
  que confirma el tesorero** (normalmente la fecha del banco), no la
  fecha de confirmación. Si esa fecha cae en un período cerrado, el
  tesorero debe reabrirlo, o el contador lo contabiliza en el período
  abierto con una referencia.

Ejemplo: 105 reporta una transferencia de 432.000 hecha el 28 de abril;
el tesorero la confirma el 2 de mayo contra el extracto bancario → `RC`
con fecha 28 de abril, si abril está abierto.

### R5 — Sobrepago y saldo a favor

Cualquier parte de un pago que no se aplique a cargos abiertos pasa a
`credit.owner [cuenta]` (R3). Cuando se emiten nuevos cargos, el módulo
de facturación aplica los saldos a favor automáticamente (los más
antiguos primero):

| Débito                     | Crédito                             |
| ----------------------------- | --------------------------------------- |
| `credit.owner [cuenta]`       | `receivable.<rol> [cuenta]`             |

Ejemplo: 408 paga 500.000 contra una cuota de 432.000 → 68.000 de saldo
a favor, aplicado a la cuota de mayo cuando se emite.

### R6 — Consignación por identificar

Aparece una consignación en el extracto bancario y nadie la ha
reclamado:

| Débito              | Crédito                 |
| ---------------------- | --------------------------- |
| `cash.bank [banco]`    | `receipt.unidentified`      |

Cuando se identifica, un asiento originado en facturación la traslada a
la cuenta:

| Débito                    | Crédito                                          |
| ---------------------------- | ----------------------------------------------------- |
| `receipt.unidentified`       | `receivable.<rol> [cuenta]` / `credit.owner [cuenta]`  |

### R7 — Intereses de mora

Se ejecuta mensualmente (política P3). Para cada cuenta de cobro, la
base es el **capital** vencido (ordinario, extraordinario, otros — nunca
intereses) en cada día del mes, usando los pagos por su fecha de pago.

| Débito                           | Crédito              |
| ------------------------------------ | ------------------------ |
| `receivable.interest [cuenta]`       | `income.interest`         |

Si un pago confirmado más tarde (R4) tiene fecha anterior a la corrida de
intereses, el interés que ese pago habría evitado se reversa
automáticamente (R13 sobre ese cargo de interés), y el resultado de la
corrida se recalcula para esa cuenta.

### R8 — Descuento por pronto pago (cuando está activo, P5)

Se aplica cuando un pago hecho en la fecha límite del descuento o antes
cubre el cargo:

| Débito              | Crédito                           |
| ---------------------- | -------------------------------------- |
| `income.discount`      | `receivable.ordinary [cuenta]`          |

### R9 — Sanciones y otros cargos

Como R1, con `receivable.fine` / `income.fine` o `receivable.other` /
`income.common_areas`, etc. Las sanciones requieren una referencia a la
decisión que las impuso.

### R10 — Factura de proveedor o gasto (causación)

Asiento `CP`, con retención cuando aplique (las tasas se configuran por
concepto de retención; las cifras siguientes son ilustrativas):

| Débito                  | Crédito                              |
| --------------------------- | ----------------------------------------- |
| `expense.<tipo>`            | `tax.withholding.income`                   |
|                              | `tax.withholding.ica` (si aplica)          |
|                              | `payable.supplier [proveedor]` (neto)      |

### R11 — Pago a un proveedor (egreso)

Asiento `CE`: Db `payable.supplier [proveedor]` / Cr `cash.bank [banco]`.
Si el gasto se paga desde un fondo extraordinario, la misma transacción
libera el fondo (segundo asiento de R2).

Los gastos bancarios y el gravamen a los movimientos financieros (GMF,
4×1000) que cobra el banco se registran a partir del extracto bancario:
Db `expense.bank` / Cr `cash.bank`.

### R12 — Gasto pagado por el administrador, reembolsado después

| Evento         | Débito              | Crédito                        |
| ---------------- | ---------------------- | ----------------------------------- |
| Gasto             | `expense.<tipo>`       | `payable.misc [administrador]`       |
| Reembolso         | `payable.misc [administrador]` | `cash.box` / `cash.bank`     |

### R13 — Reversión y corrección

Una reversión es un asiento nuevo, con tipo de comprobante `NC` (o el
tipo del original, para documentos de facturación), con cada línea del
original invertida, con fecha en un período abierto, referenciando al
original. El documento fuente queda marcado como reversado. Una
corrección es una reversión más un nuevo documento correcto. Anular una
cuota reversa cada uno de sus cargos; los cargos con aplicaciones deben
tener sus aplicaciones reversadas primero, lo que devuelve esos montos a
saldos a favor.

### R14 — Devolución de un saldo a favor

Asiento `CE`: Db `credit.owner [cuenta]` / Cr `cash.bank [banco]`.

### R15 — Apropiación del fondo de imprevistos (política P1)

Asiento mensual `NC` por el porcentaje configurado del presupuesto
ordinario: Db `expense.contingency` / Cr `fund.contingency`. Si el fondo
tiene su propia cuenta bancaria, sigue una transferencia: Db
`cash.bank.restricted` / Cr `cash.bank`. **[contador]**: tratamiento por
confirmar.

### R16 — Saldos de apertura

Un asiento `AP` con fecha el primer día del primer período administrado
en ProHo, que reproduce el balance de prueba anterior, con las cuentas
por cobrar y los saldos a favor desglosados por cuenta de cobro **y por
cargo abierto**, de modo que los estados de cuenta y la cartera vencida
funcionen desde el primer día. La importación solo se acepta cuando:

- el asiento balancea por sí solo y `opening.clearing` termina en cero;
- las cuentas por cobrar por cuenta de cobro coinciden con el estado de
  cuenta de la administración anterior para cada unidad;
- los saldos de caja y banco coinciden con los extractos bancarios en
  esa fecha.

### R17 — Deterioro de cartera (política P7)

Db `expense.impairment` / Cr `receivable.allowance`. La cuenta por
cobrar en sí no se toca: la unidad sigue debiendo el dinero, y el estado
de cuenta lo sigue mostrando.

### R18 — Cierre de fin de año

Asiento `CI` el último día del año: toda cuenta de ingreso y gasto se
lleva a cero contra `result.surplus` o `result.deficit`. La apertura del
año siguiente es el saldo de las cuentas permanentes; no se necesita un
nuevo asiento `AP`.

## Mes resuelto como ejemplo: Los Robles, abril de 2026

A partir de `database/seed/demo-community.json`, más una factura de
proveedor y la apropiación del fondo de imprevistos. Las cifras de
retención y GMF son ilustrativas.

| Comprobante | Fecha | Evento                                         | Débito                                   | Crédito                                    | Monto      |
| ----------- | ----- | ------------------------------------------------ | ------------------------------------------- | ---------------------------------------------- | ---------: |
| CA-1        | 04-01 | Cuota ordinaria de abril (R1)                     | 130505 Cuotas ordinarias [6 cuentas]        | 4105 Cuotas ordinarias                         | 3.000.000  |
| CA-2        | 04-01 | Extraordinaria, zonas comunes (R2)                | 130510 Cuotas extraordinarias [6]           | 2905 Fondos destinación específica             | 20.000.000 |
| RC-1        | 04-10 | 509 paga por transferencia (R3)                   | 111005 Bancos                               | 130505 [509]                                   | 570.000    |
| RC-2        | 04-15 | 702 paga en efectivo (R3)                         | 110505 Caja general                         | 130505 [702]                                   | 696.000    |
| CP-1        | 04-18 | Factura de mantenimiento del ascensor (R10)       | 5145 Mantenimiento 400.000                  | 2365 Retención 16.000 · 2335 Proveedores 384.000 | 400.000  |
| CE-1        | 04-20 | Pago al proveedor del ascensor (R11)              | 2335 Proveedores                            | 111005 Bancos                                  | 384.000    |
| NC-1        | 04-20 | GMF sobre ese pago (R11)                          | 5305 Gastos bancarios                       | 111005 Bancos                                  | 1.536      |
| NC-2        | 04-30 | Fondo de imprevistos, 1 % de 36.000.000/12 (R15)  | 5395 Apropiación imprevistos                | 3305 Fondo de imprevistos                      | 30.000     |

El pago de 105 reportado el 28 de abril sigue en estado `submitted` el
30 de abril: todavía sin asiento (R4).

### Balance de prueba, 30 de abril

| Código | Cuenta                             | Débito      | Crédito     |
| ------ | ------------------------------------ | ---------: | ---------: |
| 110505 | Caja general                         | 696.000    |            |
| 111005 | Bancos                               | 184.464    |            |
| 130505 | Cuotas ordinarias por cobrar         | 1.734.000  |            |
| 130510 | Cuotas extraordinarias por cobrar    | 20.000.000 |            |
| 2335   | Proveedores                          |            | 0          |
| 2365   | Retención en la fuente               |            | 16.000     |
| 2905   | Fondos destinación específica        |            | 20.000.000 |
| 3305   | Fondo de imprevistos                 |            | 30.000     |
| 4105   | Cuotas ordinarias                    |            | 3.000.000  |
| 5145   | Mantenimiento                        | 400.000    |            |
| 5305   | Gastos bancarios                     | 1.536      |            |
| 5395   | Apropiación imprevistos              | 30.000     |            |
|        | **Total**                             | **23.046.000** | **23.046.000** |

### Subsistema auxiliar de cuentas por cobrar, 30 de abril (invariante L6)

| Cuenta de cobro | 130505 Ordinarias | 130510 Extraordinarias | Total       |
| ------------------ | -----------------: | ------------------------: | ----------: |
| 105                 | 432.000            | 2.880.000                 | 3.312.000   |
| 212                 | 390.000            | 2.600.000                 | 2.990.000   |
| 307                 | 480.000            | 3.200.000                 | 3.680.000   |
| 408                 | 432.000            | 2.880.000                 | 3.312.000   |
| 509                 | 0                  | 3.800.000                 | 3.800.000   |
| 702                 | 0                  | 4.640.000                 | 4.640.000   |
| **Total**           | **1.734.000**      | **20.000.000**            | **21.734.000** |

Los totales son iguales a las cuentas de control del balance de prueba.

### Estados de cuenta, abril

- **Ingresos y gastos:** ingresos 3.000.000; gastos 431.536; excedente
  2.568.464.
- **Balance general:** activos 22.614.464 = pasivos 20.016.000 +
  patrimonio 2.598.464 (fondo de imprevistos 30.000 + excedente del
  período 2.568.464).

Estas cifras son el resultado esperado de la primera prueba de
contabilidad de extremo a extremo del backend.
