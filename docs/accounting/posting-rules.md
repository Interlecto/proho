# Posting rules

How every financial event becomes a journal entry. Accounts are written as
roles (see [chart-of-accounts.md](chart-of-accounts.md)); codes in the
examples are the template defaults. Billing-account subledger keys are shown
in brackets: `receivable.ordinary [509]`.

General principles:

- **One source document, one entry.** Each charge, payment, report
  confirmation, supplier invoice or disbursement produces exactly one entry,
  posted in the same database transaction as the document.
- **Corrections never edit.** Every correction is a reversal (rule R13),
  optionally followed by a new, correct entry.
- **Receivables move only through billing** (ledger invariant L8).
- **Amounts** are COP with no decimals in the examples.

## Rules

### R1 — Issue an ordinary assessment

One `CA` entry for the whole run, with one receivable line per billing
account.

| Debit                              | Credit            |
| ---------------------------------- | ----------------- |
| `receivable.ordinary [each account]` | `income.ordinary` |

Los Robles, April: 3.000.000 split by coefficient →
105: 432.000 · 212: 390.000 · 307: 480.000 · 408: 432.000 · 509: 570.000 ·
702: 696.000.

### R2 — Issue an extraordinary assessment

Default policy P2 (restricted fund):

| Debit                                   | Credit               |
| --------------------------------------- | -------------------- |
| `receivable.extraordinary [each account]` | `fund.extraordinary` |

When approved works are executed (R11), the fund is released to income for
the executed amount:

| Debit                | Credit                 |
| -------------------- | ---------------------- |
| `fund.extraordinary` | `income.extraordinary` |

With the alternative policy (income when issued), the credit in the first
entry is `income.extraordinary` and there is no release.

Installments: one charge, and one entry, per installment, on its issue date.

### R3 — Receive a payment (recorded by staff)

An `RC` entry built from the payment's allocations:

| Debit                             | Credit                                         |
| --------------------------------- | ---------------------------------------------- |
| `cash.box` or `cash.bank [bank]`  | one line per receivable role allocated `[account]` |
|                                   | `credit.owner [account]` for any unallocated remainder |

Example: 509 pays its April fee by transfer, 570.000 →
Dr `cash.bank` 570.000 / Cr `receivable.ordinary [509]` 570.000.

### R4 — Resident-reported payment

- `submitted`: no entry.
- `rejected`: no entry; the report is closed with a reason the resident sees.
- `confirmed`: exactly R3, dated with the **payment date the treasurer
  confirms** (normally the bank date), not the confirmation date. If that
  date falls in a closed period, the treasurer must reopen it, or the
  accountant posts it in the open period with a reference.

Example: 105 reports a 432.000 transfer made on 28 April; the treasurer
confirms it on 2 May against the bank statement → `RC` dated 28 April if
April is open.

### R5 — Overpayment and credit balance (*saldo a favor*)

Any part of a payment not allocated to open charges goes to
`credit.owner [account]` (R3). When new charges are issued, the billing
module applies credit balances automatically (oldest first):

| Debit                    | Credit                          |
| ------------------------ | ------------------------------- |
| `credit.owner [account]` | `receivable.<role> [account]`   |

Example: 408 pays 500.000 against a 432.000 fee → 68.000 credit, applied to
May's fee when it is issued.

### R6 — Unidentified deposit (*consignación por identificar*)

A deposit appears on the bank statement and nobody has claimed it:

| Debit             | Credit                 |
| ----------------- | ---------------------- |
| `cash.bank [bank]` | `receipt.unidentified` |

When identified, a billing-sourced entry moves it to the account:

| Debit                  | Credit                                     |
| ---------------------- | ------------------------------------------ |
| `receipt.unidentified` | `receivable.<role> [account]` / `credit.owner [account]` |

### R7 — Late-payment interest

Run monthly (policy P3). For each billing account, the base is the overdue
**capital** (ordinary, extraordinary, other — never interest) on each day of
the month, using payments by their payment date.

| Debit                           | Credit            |
| ------------------------------- | ----------------- |
| `receivable.interest [account]` | `income.interest` |

If a payment confirmed later (R4) is dated before the interest run, the
interest it would have avoided is reversed automatically (R13 on that
interest charge), and the run's result is recomputed for that account.

### R8 — Early-payment discount (when enabled, P5)

Applied when a payment on or before the discount date covers the charge:

| Debit             | Credit                          |
| ----------------- | ------------------------------- |
| `income.discount` | `receivable.ordinary [account]` |

### R9 — Fines and other charges

As R1, with `receivable.fine` / `income.fine` or `receivable.other` /
`income.common_areas`, etc. Fines require a reference to the decision that
imposed them.

### R10 — Supplier invoice or expense (*causación*)

`CP` entry, with withholding when applicable (rates are configured per
withholding concept; the numbers below are illustrative):

| Debit                  | Credit                       |
| ---------------------- | ---------------------------- |
| `expense.<kind>`       | `tax.withholding.income`     |
|                        | `tax.withholding.ica` (if any) |
|                        | `payable.supplier [supplier]` (net) |

### R11 — Payment to a supplier (*egreso*)

`CE` entry: Dr `payable.supplier [supplier]` / Cr `cash.bank [bank]`. If the
expense is paid from an extraordinary fund, the same transaction releases the
fund (second entry in R2).

Bank charges and the financial transactions tax (GMF, 4×1000) taken by the
bank are recorded from the bank statement: Dr `expense.bank` /
Cr `cash.bank`.

### R12 — Expense paid by the administrator, reimbursed later

| Event          | Debit             | Credit                     |
| -------------- | ----------------- | -------------------------- |
| Expense        | `expense.<kind>`  | `payable.misc [administrator]` |
| Reimbursement  | `payable.misc [administrator]` | `cash.box` / `cash.bank` |

### R13 — Reversal and correction

A reversal is a new entry, voucher type `NC` (or the original's type for
billing documents), with every line of the original inverted, dated in an
open period, referencing the original. The source document is marked
reversed. A correction is reversal + new correct document. Cancelling an
assessment reverses every one of its charges; charges with allocations must
have their allocations reversed first, which returns those amounts to credit
balances.

### R14 — Refund of a credit balance

`CE` entry: Dr `credit.owner [account]` / Cr `cash.bank [bank]`.

### R15 — Contingency fund appropriation (policy P1)

Monthly `NC` entry for the configured percentage of the ordinary budget:
Dr `expense.contingency` / Cr `fund.contingency`. If the fund has its own
bank account, a transfer follows: Dr `cash.bank.restricted` /
Cr `cash.bank`. **[accountant]**: treatment to be confirmed.

### R16 — Opening balances

An `AP` entry dated the first day of the first period managed in ProHo,
reproducing the previous trial balance, with receivables and credit balances
broken down by billing account **and by open charge**, so that statements and
aging work from day one. The import is accepted only when:

- the entry balances on its own and `opening.clearing` ends at zero;
- the receivables per billing account match the previous administration's
  statement for each unit;
- the cash and bank balances match the bank statements at that date.

### R17 — Bad-debt impairment (policy P7)

Dr `expense.impairment` / Cr `receivable.allowance`. The receivable itself is
untouched: the unit still owes the money, and the statement still shows it.

### R18 — Year-end close

`CI` entry on the last day of the year: every income and expense account to
zero against `result.surplus` or `result.deficit`. The next year's opening is
the balance of permanent accounts; no new `AP` entry is needed.

## Worked month: Los Robles, April 2026

From `database/seed/demo-community.json`, plus one supplier invoice and the
contingency appropriation. Withholding and GMF figures are illustrative.

| Voucher | Date  | Event                                     | Debit                                  | Credit                                  | Amount     |
| ------- | ----- | ----------------------------------------- | -------------------------------------- | --------------------------------------- | ---------: |
| CA-1    | 04-01 | April ordinary fee (R1)                    | 130505 Cuotas ordinarias [6 accounts]  | 4105 Cuotas ordinarias                  | 3.000.000  |
| CA-2    | 04-01 | Extraordinary, common areas (R2)           | 130510 Cuotas extraordinarias [6]      | 2905 Fondos destinación específica      | 20.000.000 |
| RC-1    | 04-10 | 509 pays by transfer (R3)                  | 111005 Bancos                          | 130505 [509]                            | 570.000    |
| RC-2    | 04-15 | 702 pays in cash (R3)                      | 110505 Caja general                    | 130505 [702]                            | 696.000    |
| CP-1    | 04-18 | Elevator maintenance invoice (R10)         | 5145 Mantenimiento 400.000             | 2365 Retención 16.000 · 2335 Proveedores 384.000 | 400.000 |
| CE-1    | 04-20 | Pay elevator supplier (R11)                | 2335 Proveedores                       | 111005 Bancos                           | 384.000    |
| NC-1    | 04-20 | GMF on that payment (R11)                  | 5305 Gastos bancarios                  | 111005 Bancos                           | 1.536      |
| NC-2    | 04-30 | Contingency fund, 1% of 36.000.000/12 (R15) | 5395 Apropiación imprevistos          | 3305 Fondo de imprevistos               | 30.000     |

The 105 payment reported on 28 April is still `submitted` on 30 April: no
entry yet (R4).

### Trial balance, 30 April

| Code   | Account                         | Debit      | Credit     |
| ------ | ------------------------------- | ---------: | ---------: |
| 110505 | Caja general                    | 696.000    |            |
| 111005 | Bancos                          | 184.464    |            |
| 130505 | Cuotas ordinarias por cobrar    | 1.734.000  |            |
| 130510 | Cuotas extraordinarias por cobrar | 20.000.000 |          |
| 2335   | Proveedores                     |            | 0          |
| 2365   | Retención en la fuente          |            | 16.000     |
| 2905   | Fondos destinación específica   |            | 20.000.000 |
| 3305   | Fondo de imprevistos            |            | 30.000     |
| 4105   | Cuotas ordinarias               |            | 3.000.000  |
| 5145   | Mantenimiento                   | 400.000    |            |
| 5305   | Gastos bancarios                | 1.536      |            |
| 5395   | Apropiación imprevistos         | 30.000     |            |
|        | **Total**                       | **23.046.000** | **23.046.000** |

### Receivables subledger, 30 April (invariant L6)

| Billing account | 130505 Ordinarias | 130510 Extraordinarias | Total       |
| --------------- | ----------------: | ---------------------: | ----------: |
| 105             | 432.000           | 2.880.000              | 3.312.000   |
| 212             | 390.000           | 2.600.000              | 2.990.000   |
| 307             | 480.000           | 3.200.000              | 3.680.000   |
| 408             | 432.000           | 2.880.000              | 3.312.000   |
| 509             | 0                 | 3.800.000              | 3.800.000   |
| 702             | 0                 | 4.640.000              | 4.640.000   |
| **Total**       | **1.734.000**     | **20.000.000**         | **21.734.000** |

The totals equal the control accounts in the trial balance.

### Statements, April

- **Income and expenses:** income 3.000.000; expenses 431.536; surplus
  2.568.464.
- **Balance sheet:** assets 22.614.464 = liabilities 20.016.000 + equity
  2.598.464 (contingency fund 30.000 + period surplus 2.568.464).

These figures are the expected results of the backend's first end-to-end
accounting test.
