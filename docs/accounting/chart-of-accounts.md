# Default chart of accounts

A template for Colombian residential communities, with PUC-style codes. Each
community gets a copy it can rename, extend and renumber. **[accountant]**:
the codes and the treatment of funds (classes 28 and 33) must be validated.

Posting rules never refer to codes. They refer to **roles** (right-hand
column), which each community maps to one of its postable accounts. A role
can have sub-accounts (e.g. one bank account per real bank account).

## 1 Activo

| Code   | Account                                           | Role                     |
| ------ | ------------------------------------------------- | ------------------------ |
| 1105   | Caja                                              |                          |
| 110505 | Caja general                                      | `cash.box`               |
| 110510 | Cajas menores                                     | `cash.petty`             |
| 1110   | Bancos                                            |                          |
| 111005 | Cuenta de ahorros / corriente operativa           | `cash.bank`              |
| 111010 | Cuenta del fondo de imprevistos (restringida)     | `cash.bank.restricted`   |
| 1305   | Cuotas y cobros por cobrar a copropietarios       |                          |
| 130505 | Cuotas ordinarias de administración               | `receivable.ordinary`    |
| 130510 | Cuotas extraordinarias                            | `receivable.extraordinary` |
| 130515 | Intereses de mora                                 | `receivable.interest`    |
| 130520 | Sanciones                                         | `receivable.fine`        |
| 130595 | Otros cobros (uso de zonas comunes, etc.)         | `receivable.other`       |
| 1330   | Anticipos y avances a proveedores                 | `advance.supplier`       |
| 1355   | Anticipo de impuestos y retenciones a favor       | `tax.receivable`         |
| 1380   | Deudores varios                                   | `receivable.misc`        |
| 1399   | Deterioro de cuentas por cobrar (CR)              | `receivable.allowance`   |
| 1524   | Equipo de oficina                                 | `asset.equipment`        |
| 1528   | Equipo de cómputo y comunicación                  | `asset.computers`        |
| 1592   | Depreciación acumulada (CR)                       | `asset.depreciation`     |

## 2 Pasivo

| Code   | Account                                           | Role                     |
| ------ | ------------------------------------------------- | ------------------------ |
| 2335   | Costos y gastos por pagar (proveedores)           | `payable.supplier`       |
| 2365   | Retención en la fuente por pagar                  | `tax.withholding.income` |
| 2368   | Retención de industria y comercio por pagar       | `tax.withholding.ica`    |
| 2370   | Retenciones y aportes de nómina                   | `payroll.withholding`    |
| 2380   | Acreedores varios (incluye reembolsos al administrador) | `payable.misc`     |
| 2505   | Salarios y prestaciones por pagar                 | `payroll.payable`        |
| 2805   | Anticipos recibidos — saldos a favor de copropietarios | `credit.owner`      |
| 289505 | Consignaciones por identificar                    | `receipt.unidentified`   |
| 2905   | Fondos con destinación específica (cuotas extraordinarias) | `fund.extraordinary` |

## 3 Patrimonio

| Code   | Account                                           | Role                     |
| ------ | ------------------------------------------------- | ------------------------ |
| 3305   | Fondo de imprevistos (Ley 675, art. 35)           | `fund.contingency`       |
| 3605   | Excedente del ejercicio                           | `result.surplus`         |
| 3610   | Déficit del ejercicio                             | `result.deficit`         |
| 3705   | Resultados de ejercicios anteriores               | `result.prior`           |
| 3999   | Saldos de apertura (transitoria, debe quedar en cero) | `opening.clearing`   |

## 4 Ingresos

| Code   | Account                                           | Role                     |
| ------ | ------------------------------------------------- | ------------------------ |
| 4105   | Cuotas ordinarias de administración               | `income.ordinary`        |
| 4110   | Cuotas extraordinarias ejecutadas                 | `income.extraordinary`   |
| 4115   | Intereses de mora                                 | `income.interest`        |
| 4120   | Sanciones                                         | `income.fine`            |
| 4125   | Uso de zonas comunes (parqueaderos, salón comunal) | `income.common_areas`   |
| 4175   | Descuentos por pronto pago (DB)                   | `income.discount`        |
| 4210   | Rendimientos financieros                          | `income.financial`       |
| 4250   | Recuperaciones                                    | `income.recoveries`      |
| 4295   | Ingresos diversos                                 | `income.other`           |

## 5 Gastos

| Code   | Account                                           | Role                     |
| ------ | ------------------------------------------------- | ------------------------ |
| 5105   | Gastos de personal                                | `expense.payroll`        |
| 5110   | Honorarios (administración, contador, revisor fiscal) | `expense.fees`       |
| 5115   | Impuestos                                         | `expense.taxes`          |
| 5130   | Seguros (póliza de bienes comunes)                | `expense.insurance`      |
| 5135   | Servicios (vigilancia, aseo, servicios públicos comunes) | `expense.services` |
| 5145   | Mantenimiento y reparaciones                      | `expense.maintenance`    |
| 5160   | Depreciaciones                                    | `expense.depreciation`   |
| 5195   | Diversos                                          | `expense.other`          |
| 5199   | Deterioro de cuentas por cobrar                   | `expense.impairment`     |
| 5305   | Gastos bancarios y GMF (4×1000)                   | `expense.bank`           |
| 5395   | Apropiación fondo de imprevistos                  | `expense.contingency`    |
