# Accounting

Status: **provisional** (Phase 0). Must be validated by a Colombian
public accountant (*contador público*) before Phase 2 is implemented. Points
that need that validation are marked **[accountant]**.

| Document                                      | Contents                                         |
| --------------------------------------------- | ------------------------------------------------ |
| [chart-of-accounts.md](chart-of-accounts.md)  | Default chart of accounts template              |
| [posting-rules.md](posting-rules.md)          | Journal entries for every financial event, with a worked month |
| This file                                     | Framework, periods, vouchers, ledger invariants, policy decisions |

The domain entities referred to here (billing account, charge, payment,
allocation, payment report) are defined in
[`docs/architecture/domain-model.md`](../architecture/domain-model.md).

## 1. Framework

The first communities are Colombian residential *propiedad horizontal*:

- **Ley 675 de 2001.** The community is a non-profit legal entity. The
  assembly approves the annual budget, extraordinary assessments and the
  financial statements (art. 38); the administrator is its legal
  representative. Relevant rules: the debt follows the unit and owner and
  tenant are jointly liable (art. 29), late-payment interest (art. 30), the
  contingency fund of at least 1% of the annual budget (art. 35), and the
  statutory auditor (art. 56; mandatory for commercial or mixed use).
- **Accounting standards.** Decreto 2420 de 2015 and its amendments (NIIF
  framework). Most residential communities fall in Group 3 (simplified
  accounting for micro-entities) or Group 2 (NIIF para PYMES), depending on
  size. The CTCP's technical guidance for *copropiedades* (Orientación
  Técnica 15) is the reference for specific treatments. **[accountant]**:
  confirm the group of the pilot community and the applicable guidance.
- **Account codes.** The platform ships a template with PUC-style codes
  (Decreto 2650 de 1993 numbering), which accountants in Colombia expect,
  but the chart is fully configurable per community. Codes carry no logic:
  posting rules refer to accounts by **role** (`receivable.ordinary`,
  `cash.bank`, …), and each community maps roles to its own accounts.
- **Taxes.** Residential communities are not income-tax payers on their
  common-property activity, but they are withholding agents
  (*agentes de retención*) when paying suppliers. **[accountant]**: confirm,
  and decide whether withholding is in the MVP (recommended: yes, basic).
  Tax filing and issuing electronic invoices are out of scope; recording
  received supplier invoices is in scope.

## 2. Fiscal years, periods and vouchers

- **Fiscal year:** calendar year, divided into monthly periods.
- **Period states:**
  - `open`: postings allowed.
  - `closed`: no postings; the accountant can reopen it, which is audited.
  - `locked`: financial statements approved by the assembly; cannot reopen.
    Corrections go into the current open period with a reference to the
    original entry.
- **Year-end close:** a closing entry moves income and expense balances to
  the year's surplus or deficit, and the next year opens.
- **Vouchers (*comprobantes*):** every journal entry has a voucher type and a
  gapless number per community, type and fiscal year:

  | Type | Name                         | Produced by                        |
  | ---- | ---------------------------- | ---------------------------------- |
  | `CA` | Comprobante de causación     | Issuing charges (assessments, interest, fines) |
  | `RC` | Recibo de caja               | Payments received                  |
  | `CE` | Comprobante de egreso        | Payments to suppliers, refunds     |
  | `CP` | Cuenta por pagar / causación de gasto | Recording supplier invoices and expenses |
  | `NC` | Nota contable                | Adjustments, reversals, reclassifications |
  | `AP` | Apertura                     | Opening balances                   |
  | `CI` | Cierre                       | Year-end close                     |

## 3. Ledger invariants

The backend enforces these in the posting service and in database
constraints, and tests each one. They extend the domain invariants in
`domain-model.md` §9.

| #   | Invariant                                                                 |
| --- | ------------------------------------------------------------------------- |
| L1  | Every journal entry has at least two lines and Σ debits = Σ credits > 0.  |
| L2  | Lines post only to active, postable (leaf) accounts of the entry's community. |
| L3  | The entry date falls in an `open` period of the entry's community.         |
| L4  | Posted entries and lines are never updated or deleted. A reversal is a new entry with every line inverted, referencing the original; an entry can be reversed at most once. |
| L5  | Every line on a receivables account carries a billing account; every line on a cash or bank account carries the cash box or bank account. |
| L6  | For each receivables account, the balance equals the sum of its billing-account subledger balances; the same for credit balances (*saldos a favor*). |
| L7  | Every entry produced by the billing module references its source document (charge, payment, allocation, report confirmation), and every posted source document has exactly one entry (plus its reversal, if reversed). |
| L8  | Manual entries (`NC`) cannot post to receivables or credit-balance accounts. Those balances change only through billing documents, so statements and ledger never diverge. |
| L9  | Voucher numbers are unique and gapless per community, type and fiscal year. A failed transaction consumes no number. |
| L10 | Every report (trial balance, ledger, statements, member statements, aging) is computed from journal lines only, never from separately stored totals. |
| L11 | The opening entry balances on its own; a temporary opening account, if used during import, must end at zero. |

## 4. Policy decisions

Each community configures these; the template ships a default.

| #  | Decision                                  | Default                                              |
| -- | ----------------------------------------- | ---------------------------------------------------- |
| P1 | Contingency fund (*fondo de imprevistos*, art. 35) | Monthly appropriation of a configured percentage (≥ 1%) of the ordinary budget into a restricted fund. **[accountant]**: equity reserve or liability, and whether funded by a separate bank account. |
| P2 | Extraordinary assessments                  | Recognized as restricted fund (`fund.extraordinary`) when issued, and transferred to income as the approved works are executed. Simpler alternative: income when issued. **[accountant]** |
| P3 | Late-payment interest (art. 30)             | Monthly, on overdue **capital** only (no interest on interest), at the configured rate, never above the usury rate certified by the Superintendencia Financiera for that month. |
| P4 | Payment allocation order                    | Interest first, then oldest due charge (Código Civil art. 1653). |
| P5 | Early-payment discount (*pronto pago*)      | Off. When on: a percentage or amount if paid by a date, posted as a reduction of income. |
| P6 | Rounding                                    | Charges rounded to the nearest peso; largest remainder keeps totals exact. Configurable to 100 pesos. |
| P7 | Bad debts (*deterioro de cartera*)          | Manual, by the accountant, through an impairment allowance. Never by deleting charges. |
| P8 | Revenue from common areas (parking, hall rental) | Income when the service is billed.            |
