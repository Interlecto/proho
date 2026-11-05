# Contabilidad

*[Read in English](README.md)*

Estado: **provisional** (Fase 0). Debe ser validado por un contador
público colombiano antes de implementar la Fase 2. Los puntos que
necesitan esa validación están marcados **[contador]**.

| Documento                                              | Contenido                                                        |
| ------------------------------------------------------ | ----------------------------------------------------------------- |
| [plan-de-cuentas.md](plan-de-cuentas.md)                | Plantilla predeterminada del plan de cuentas                     |
| [reglas-de-contabilizacion.md](reglas-de-contabilizacion.md) | Asientos contables para cada evento financiero, con un mes resuelto como ejemplo |
| Este archivo                                            | Marco normativo, períodos, comprobantes, invariantes del libro mayor, decisiones de política contable |

Las entidades de dominio mencionadas aquí (cuenta de cobro, cargo, pago,
aplicación, reporte de pago) están definidas en
[`docs/architecture/domain-model.md`](../architecture/domain-model.md)
(en inglés).

**Restricción entre proyectos:** el módulo de contabilidad debe
mantenerse compatible e intercambiable con
[Contarte](https://github.com/chlewey/Contarte) (copia local:
`C:\Temp\Contarte`), la aplicación personal de contabilidad de partida
doble para Android del autor. El plan de cuentas, los asientos contables
y los invariantes del libro mayor anteriores deben compartir un modelo de
datos con Contarte, o tener una correspondencia exacta y sin pérdida de
información con él, además de un formato común de importación y
exportación, de modo que un conjunto de libros pueda moverse entre
ambas aplicaciones sin necesidad de conciliación. Contarte almacena el
dinero como unidades enteras mínimas por moneda (nunca en coma
flotante); el libro mayor de ProHo debe seguir la misma representación.
La exportación/importación CSV que Contarte tiene planeada (su hito M5)
es una candidata para ese formato común: hay que seguir su modelo de
datos a medida que se estabilice, en lugar de alejarse de él.

## 1. Marco normativo

Las primeras comunidades son conjuntos de propiedad horizontal
residencial en Colombia:

- **Ley 675 de 2001.** La comunidad es una persona jurídica sin ánimo de
  lucro. La asamblea aprueba el presupuesto anual, las cuotas
  extraordinarias y los estados financieros (art. 38); el administrador
  es su representante legal. Reglas relevantes: la deuda sigue a la
  unidad y el propietario y el arrendatario son solidariamente
  responsables (art. 29), los intereses de mora (art. 30), el fondo de
  imprevistos de al menos el 1 % del presupuesto anual (art. 35), y el
  revisor fiscal (art. 56; obligatorio para uso comercial o mixto).
- **Normas contables.** Decreto 2420 de 2015 y sus modificaciones (marco
  NIIF). La mayoría de las comunidades residenciales se ubican en el
  Grupo 3 (contabilidad simplificada para microempresas) o en el Grupo 2
  (NIIF para PYMES), según su tamaño. La orientación técnica del CTCP
  para copropiedades (Orientación Técnica 15) es la referencia para
  tratamientos específicos. **[contador]**: confirmar el grupo de la
  comunidad piloto y la orientación aplicable.
- **Códigos de cuenta.** La plataforma incluye una plantilla con códigos
  de estilo PUC (numeración del Decreto 2650 de 1993), que es lo que los
  contadores en Colombia esperan, pero el plan de cuentas es totalmente
  configurable por comunidad. Los códigos no llevan lógica: las reglas
  de contabilización se refieren a las cuentas por **rol**
  (`receivable.ordinary`, `cash.bank`, …), y cada comunidad asigna sus
  roles a sus propias cuentas.
- **Impuestos.** Las comunidades residenciales no son contribuyentes del
  impuesto de renta por su actividad de bienes comunes, pero sí son
  agentes de retención al pagar a proveedores. **[contador]**: confirmar,
  y decidir si la retención forma parte del MVP (recomendado: sí, de
  forma básica). La declaración de impuestos y la emisión de factura
  electrónica están fuera de alcance; registrar las facturas recibidas de
  proveedores sí está en alcance.

## 2. Años fiscales, períodos y comprobantes

- **Año fiscal:** año calendario, dividido en períodos mensuales.
- **Estados del período:**
  - `open` (abierto): se permiten contabilizaciones.
  - `closed` (cerrado): no se permiten contabilizaciones; el contador
    puede reabrirlo, lo cual queda auditado.
  - `locked` (bloqueado): estados financieros aprobados por la asamblea;
    no se puede reabrir. Las correcciones se registran en el período
    abierto vigente, con una referencia al asiento original.
- **Cierre de fin de año:** un asiento de cierre traslada los saldos de
  ingresos y gastos al excedente o déficit del año, y se abre el año
  siguiente.
- **Comprobantes:** cada asiento contable tiene un tipo de comprobante y
  un número consecutivo sin saltos por comunidad, tipo y año fiscal:

  | Tipo | Nombre                                | Generado por                                     |
  | ---- | -------------------------------------- | ------------------------------------------------- |
  | `CA` | Comprobante de causación              | Emisión de cargos (cuotas, intereses, sanciones)  |
  | `RC` | Recibo de caja                        | Pagos recibidos                                   |
  | `CE` | Comprobante de egreso                 | Pagos a proveedores, reembolsos                   |
  | `CP` | Cuenta por pagar / causación de gasto | Registro de facturas de proveedores y gastos      |
  | `NC` | Nota contable                         | Ajustes, reversiones, reclasificaciones           |
  | `AP` | Apertura                              | Saldos de apertura                                |
  | `CI` | Cierre                                 | Cierre de fin de año                              |

## 3. Invariantes del libro mayor

El backend hace cumplir estas reglas en el servicio de contabilización y
mediante restricciones de base de datos, y cada una tiene pruebas
automatizadas. Extienden los invariantes de dominio de `domain-model.md`
§9.

| #   | Invariante                                                                 |
| --- | --------------------------------------------------------------------------- |
| L1  | Todo asiento contable tiene al menos dos líneas y Σ débitos = Σ créditos > 0. |
| L2  | Las líneas solo se contabilizan en cuentas activas y contabilizables (hoja) de la comunidad del asiento. |
| L3  | La fecha del asiento cae dentro de un período `open` de la comunidad del asiento. |
| L4  | Los asientos y líneas contabilizados nunca se modifican ni se eliminan. Una reversión es un asiento nuevo con cada línea invertida, que referencia al original; un asiento puede reversarse como máximo una vez. |
| L5  | Toda línea en una cuenta por cobrar lleva una cuenta de cobro; toda línea en una cuenta de caja o banco lleva la caja menor o la cuenta bancaria correspondiente. |
| L6  | Para cada cuenta por cobrar de control, el saldo es igual a la suma de los saldos del subsistema auxiliar por cuenta de cobro; lo mismo aplica para los saldos a favor. |
| L7  | Todo asiento producido por el módulo de facturación referencia su documento fuente (cargo, pago, aplicación, confirmación de reporte), y todo documento fuente contabilizado tiene exactamente un asiento (más su reversión, si fue reversado). |
| L8  | Los asientos manuales (`NC`) no pueden contabilizarse en cuentas por cobrar ni en cuentas de saldo a favor. Esos saldos solo cambian a través de documentos de facturación, de modo que los estados de cuenta y el libro mayor nunca diverjan. |
| L9  | Los números de comprobante son únicos y sin saltos por comunidad, tipo y año fiscal. Una transacción fallida no consume ningún número. |
| L10 | Todo reporte (balance de prueba, libro mayor, estados de cuenta, estados de cuenta de miembros, cartera vencida) se calcula únicamente a partir de las líneas del libro mayor, nunca a partir de totales almacenados por separado. |
| L11 | El asiento de apertura balancea por sí solo; una cuenta transitoria de apertura, si se usa durante la importación, debe terminar en cero. |

## 4. Decisiones de política contable

Cada comunidad configura lo siguiente; la plantilla trae un valor
predeterminado.

| #  | Decisión                                           | Valor predeterminado                                                  |
| -- | --------------------------------------------------- | ------------------------------------------------------------------------ |
| P1 | Fondo de imprevistos (art. 35)                      | Apropiación mensual de un porcentaje configurado (≥ 1 %) del presupuesto ordinario hacia un fondo restringido. **[contador]**: ¿reserva patrimonial o pasivo?, y si se financia con una cuenta bancaria separada. |
| P2 | Cuotas extraordinarias                              | Se reconocen como fondo restringido (`fund.extraordinary`) al emitirse, y se trasladan a ingreso conforme se ejecutan las obras aprobadas. Alternativa más simple: ingreso al momento de emitirse. **[contador]** |
| P3 | Intereses de mora (art. 30)                         | Mensual, solo sobre el **capital** vencido (sin interés sobre interés), a la tasa configurada, nunca por encima de la tasa de usura certificada por la Superintendencia Financiera para ese mes. |
| P4 | Orden de aplicación de pagos                        | Primero los intereses, luego el cargo vencido más antiguo (Código Civil, art. 1653). |
| P5 | Descuento por pronto pago                           | Desactivado. Si se activa: un porcentaje o monto si se paga antes de una fecha, contabilizado como una reducción del ingreso. |
| P6 | Redondeo                                             | Los cargos se redondean al peso más cercano; el mayor residuo mantiene los totales exactos. Configurable a 100 pesos. |
| P7 | Deterioro de cartera                                 | Manual, a cargo del contador, mediante una provisión por deterioro. Nunca eliminando cargos. |
| P8 | Ingresos de zonas comunes (parqueaderos, salón social) | Ingreso en el momento en que se factura el servicio.                   |
