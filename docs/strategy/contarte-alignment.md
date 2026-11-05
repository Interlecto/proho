# Alignment with Contarte's strategy

Status: **exploratory**, like the documents it responds to, apart from section 1,
which lists what is decided. Everything else is a proposal or an open question, and
nothing here commits ProHo to a phase or a date.

This is ProHo's side of a conversation that Contarte's post-v1 strategy documents
started. They explicitly ask that a future session check ProHo's own docs before
assuming what ProHo would expose or require, and they note that nothing ProHo had
stated confirmed it would build such an API. This document is that check, and
section 1 now gives that confirmation.

Sources in [Contarte](https://github.com/chlewey/Contarte), read at commit
`07f45b2` (M0–M4 done, M5 next):

- `docs/strategy/server-api.md`
- `docs/strategy/usage-cases-professional.md`
- `docs/strategy/usage-cases-easy-mode.md`
- `docs/decisions/0002-proho-cross-project-accounting.md` (ADR 0002, accepted)

## 1. What is already decided

- **Contarte is upstream for the shared accounting primitives** (ADR 0002): money
  representation, the ledger → account → journal entry → line shape with dimension
  tagging, chart rules, and the posting invariants. If ProHo needs a primitive
  Contarte lacks, it is added in Contarte first and ProHo consumes it.
- **ProHo keeps the domain.** Vouchers, fiscal-year period locking, funds,
  billing-account subledgers, withholding and Colombian regulatory detail stay
  here. Contarte does not need to understand them.
- **Money is integer minor units plus a currency code**
  ([domain model §8](../architecture/domain-model.md)). The domain model previously
  said `numeric(19,2)`, which contradicted this contract. No ledger table exists
  yet, so the correction costs nothing today.
- **ProHo will expose a ledger API to Contarte clients, through AAA agreements**
  (decided 2026-11-05). Access is governed by an agreement covering authentication,
  authorization and accounting, and there is no access without one. The terms and
  the timing are still open (sections 5 and 7).
- **ProHo's accounting module API is the same contract as the Contarte API**
  (decided 2026-11-05). There is one accounting API definition, not a ProHo API plus
  an adapter for Contarte. ProHo's own web, Android and iOS clients use it too. What
  differs between callers is authorization, not the contract.

## 2. Which product serves whom

| Usage case (from Contarte's documents)      | Served by | Notes |
| ------------------------------------------- | --------- | ----- |
| Personal finances, household, freelancer, side business, small retailer | Contarte | Out of ProHo's scope. |
| Small informal association or homeowners' group | Either, then ProHo | Contarte when someone just needs the books. ProHo when members need shared access, billing and communication (free plan, `domain-model.md` §10.3; club and association kinds, §10.2). This is the most concrete use of the interchange format: books started in Contarte move to ProHo when the group outgrows one person's phone. |
| Bookkeeper with several clients             | Partly | ProHo already covers this when the clients are communities: one account, many memberships, with isolation between them. It answers Contarte's "does this belong to another product?" question only for communities. ProHo has no plan to host general small-business ledgers. |
| External accountant or fiscal auditor of a community on ProHo | ProHo as server, Contarte as client | Sections 3 to 5. |

Contarte's Friendly-mode **transaction types** (named posting templates over the
same primitives) have a close ProHo counterpart: posting rules R1–R18 and the
community kinds are in effect a sector package for *propiedad horizontal*. Nothing
here needs to change. ProHo's users work through billing documents rather than
journal lines, and permissions come from roles, so Contarte's Friendly/Pro mode
split does not carry over.

## 3. Primitive mapping

How Contarte's current model (`core/accounting/`) lines up with ProHo's documented
one. ProHo's ledger is not built yet, so the right-hand side is design, not code.

| Contarte                                   | ProHo                                  | Fit |
| ------------------------------------------ | -------------------------------------- | --- |
| Ledger: code, name, type, base currency    | One set of books per community         | Direct, as type `NonProfit`. ProHo needs a stable ledger code per community. |
| Money: `Long` minor units + currency       | Same, after this change                | Direct. Contarte registers COP with 2 fraction digits, so a whole-peso amount is stored × 100. ProHo should use the same digits, even though rounding policy P6 charges whole pesos. |
| Account: code, name, type, normal side, parent, active | Chart template with PUC-style codes | Direct. PUC codes fit Contarte's code pattern (`[A-Za-z0-9._-]+`, at most 20 characters). |
| `isPostable`: active leaf account          | L2                                     | Same rule. |
| `ChartRules`: sub-account shares parent's type, no cycles, archived parents take no active children | Respected by the template, per ADR 0002 | ProHo's chart configuration must enforce them too. It doesn't state them yet. |
| Contra accounts through `normalSide`       | E.g. the impairment allowance (P7)     | The chart template must record type and normal side for each account. |
| Account role: none                         | `receivable.ordinary`, `cash.bank`, …  | ProHo-only. Not exposed as a primitive. |
| Balanced entry, at least two lines         | L1                                     | Same rule. |
| Posted entries immutable; reversal inverts every line; at most once | L4, R13   | Same at the primitive level. See section 4 for where ProHo adds conditions. |
| Entry `consecutive`, unique per ledger; `reversesConsecutive` | Voucher type + gapless number per community, type and fiscal year (L9) | **Gap.** ProHo has no per-ledger consecutive. Proposal: also assign one, and carry the voucher (e.g. `RC-2026-000123`) in Contarte's `reference` field. |
| Draft entries (`isPosted = false`, soft-deleted) | None: entries exist only once posted | **Gap.** ProHo would neither export nor accept drafts as ledger data. Whether a draft stays on the Contarte device is a client matter. |
| `EntrySource`: manual, import, sync, api   | Source document (L7)                   | ProHo-only detail. Entries from a remote client would be `NC`, with their origin recorded in the audit event. |
| Line currency and exchange rate            | One community currency                 | ProHo would reject foreign-currency lines unless a later decision adds them. |
| Dimensions and line tags                   | None; subledger keys on lines (L5)     | **Gap, and a candidate solution.** Export the billing account and the cash or bank account as dimensions, so a generic client can slice by them. ProHo may need real dimensions for cost centers too; if so, it adopts Contarte's. |
| Periods: none                              | `open`, `closed`, `locked` (L3)        | ProHo-only. It surfaces to the client as a rejected write. |

Because the contract is shared, each **gap** above has to be settled in the contract
itself, not in a ProHo-side translation. A feature ProHo doesn't support, such as
drafts or foreign-currency lines, must be optional in the contract, so that ProHo
can conform without implementing it.

## 4. Where the strategy documents need a caveat

Contarte's documents reasonably describe the external-accountant case as needing
little new design. Reading them against ProHo's rules, four assumptions don't fully
hold. These are worth feeding back to Contarte before its M9 work starts.

1. **"Fix errors" is not always a plain reversal.** Contarte treats correction as
   posting a reversal against the remote ledger. In ProHo that holds only for
   entries with no billing document behind them:
   - L8 forbids manual entries on receivables and credit-balance accounts.
   - R13 marks the source document as reversed, and requires a charge's allocations
     to be reversed first.

   A generic reversal of a charge or payment entry must therefore be rejected, or
   routed through billing. Otherwise statements and ledger diverge, which is what
   L6 and L8 exist to prevent. The primitive boundary is the right *visibility*
   scope but not a complete *write* scope.
2. **A valid entry can still be refused.** An entry dated in a closed or locked
   period (L3), or a manual entry on a receivables account (L8), balances perfectly
   and would post in a local Contarte ledger, but ProHo must reject it. The shared
   contract needs a defined way to report a server-side rule violation, so
   Contarte's screens can show it without knowing ProHo's domain.
3. **Tier 4 sync does not fit ProHo-hosted books.** Against ProHo, writes have to
   be online, as in Contarte's tier 5. Gapless voucher numbers (L9), open-period
   checks (L3) and the server-authoritative rule in
   [ROADMAP §3.4](../../ROADMAP.md) all require it. Reads could be cached, under the
   same expiry and access-revocation rules as mobile clients. Contarte's "repository
   swap" test still applies, but only for the read path and for online writes.
4. **"Lossless" has a narrower meaning than it first sounds.**
   - *ProHo → Contarte:* the primitives survive, while roles, source documents,
     periods and fund policy do not, unless the format carries them as opaque
     extensions.
   - *Contarte → ProHo:* entries touching receivables cannot be replayed as-is,
     because L7 and L8 require billing documents. The honest route is to import
     them as opening balances (R16) plus entries on the other accounts.

   So a set of books moves without reconciliation at the level of **balances and
   reports**. ProHo's documents should promise that, not identical records.

## 5. The ledger API and its AAA agreements

The decision is made (section 1) but not scheduled. An AAA agreement has to settle
three things. The right-hand column is a proposal that fits ProHo's existing rules.

| Part           | Question                       | Proposed answer |
| -------------- | ------------------------------ | --------------- |
| Authentication | Who is calling?                | A ProHo platform account, used through a credential the Contarte client can hold. |
| Authorization  | What may they do, and where?   | A ledger-only grant bound to one community, effective-dated, in read or read-and-correct form. |
| Accounting     | What is recorded of what they did? | An AuditEvent for every write, as for any other command. "Accounting" here means this record of activity, not bookkeeping. |

**Access model.** Contarte's documents say the accountant "does not need to be a
ProHo user." ProHo's model has one account per human, and a membership currently
grants community visibility (the directory, for example), so the existing roles
are too broad:

- `accountant` also covers billing, and the *privileged* visibility tier.
- `auditor` is "read everything", beyond the ledger.

The proposal is to keep one platform account, so identity, audit and revocation
work as they do today, and add a ledger-only grant bound to one community,
effective-dated like other roles. It would come in read and read-and-correct forms,
with no directory, communications or resident access. "Not a user" then means the
accountant never uses ProHo's product, not that they have no identity in it. The
statutory *revisor fiscal* usually needs the read form; the external contador, the
correcting one.

**Data protection.** Entry descriptions are free text, which `domain-model.md`
§10.4 treats as potentially personal data, and invariant 14 restricts where
protected fields may go. Billing-account dimensions must use codes (`509`), not
names. A copy on an accountant's device falls under the offline-retention question
(ROADMAP §7, item 4). With customer-held keys, revoking access also has to stop
this path.

**Plans.** The first paid plan already promises API access (`domain-model.md`
§10.3), so this is a natural entitlement. But the same section says a community's
own books are never withheld on the free plan, so standard exports must remain
available to an accountant either way.

**Contract.**
- The accounting module's API is the shared contract (section 1), defined against
  Contarte's primitives under ADR 0002. ProHo is an independent implementation of it.
- The contract stops at the accounting module. Billing keeps its own ProHo API, and
  billing documents reach the ledger through an application service, in process, as
  [ROADMAP §5](../../ROADMAP.md) requires for payment connectors. They never post
  through the contract.
- ProHo's own accountant and an external one call the same endpoints. Membership
  roles authorize the first, and an AAA agreement the second.
- ProHo's REST/OpenAPI architecture constrains the contract's protocol, which
  Contarte's documents still list as open.
- A second implementation existing early is what Contarte's documents call the
  forcing function for spec-first. ProHo's commitment now makes that real, so
  Contarte's spec-first question should be settled with it in mind before M9 begins.
- Contarte's hosting-portability question doesn't bind ProHo. It stays a centralized
  multi-tenant service with no self-hosting goal.

**Reports.** ProHo computes every report on the server from journal lines (L10), and
Contarte computes the same figures from the same lines. Serving reports through the
API and letting the client compute them should therefore agree, which makes a good
cross-implementation acceptance test either way.

## 6. What this means for Phase 2

These cost little while the ledger is being designed, and much more afterwards.
Each is a design input for Phase 2, not new scope:

1. Store money as integer minor units (done in the domain model).
2. Record type and normal side on every account, and enforce `ChartRules`.
3. Keep the journal readable without the `billing` module's tables, so the
   primitive boundary is real in the code.
4. Give every entry a per-community consecutive alongside its voucher number.
5. Express permissions so a ledger-only grant can be added without reworking them.
6. Before finalizing the schema, re-check Contarte's for drift (ADR 0002 asks for
   the same in the other direction).

**The contract becomes a Phase 2 dependency.** Contarte's roadmap builds its API in
M9, after M8 picks a sync tier, and M9 has no date. ProHo can't implement its
accounting API against a contract that doesn't exist, so the contract has to be
written earlier than that sequence implies. Contarte's `server-api.md` already notes
that a published spec is cheap and could ship early, ahead of any server, which makes
this feasible: the spec moves up, and Contarte's server can stay in M9. Its first
version must cover what ProHo needs (online, server-authoritative writes) without
ruling out what M8 may choose for Contarte's own sync.

The interchange format itself waits for Contarte's M5 (estimated around April 2027,
at its stated pace). ProHo's Phase 2 CSV exports shouldn't wait for it, but the
journal and chart exports should keep the columns close enough to adapt.

## 7. Open questions

1. **What is an AAA agreement, concretely?** Who are its parties (the community, the
   accountant, the platform), and is there one per community? Who approves it on the
   community's side: the administrator, or the assembly? Does it need to be a legal
   document as well as a configuration record? **[legal]**: the community is the data
   controller under Ley 1581 de 2012, so the agreement must rest on its authority.
   Is it a paid entitlement, or also available on the free plan?
2. **Which write scope?** Read-only first, with corrections limited to entries with
   no billing document, seems the safe start. Should correcting a billing entry
   ever be possible from a generic client?
3. **Where does the contract live, and who can change it?** ADR 0002 points to
   Contarte's repository, with ProHo's `api/openapi/openapi.yaml` referencing or
   vendoring it. How is it versioned, and how does ProHo pin a version? Its first
   concrete questions are the wire format for money (integer minor units fit both
   projects, and floating point is ruled out either way) and whether reports are part
   of it.
4. **Is the reduced meaning of "lossless"** in section 4 acceptable, or does the
   format need extension columns so ProHo's metadata can round-trip through Contarte?
5. **Dimensions in ProHo.** Are they needed for cost centers or works, or only as an
   export projection of subledger keys?
6. **ProHo-only accounting operations.** Closing periods, mapping roles to accounts,
   looking up vouchers and importing opening balances have no Contarte equivalent.
   Do they sit beside the contract as a clearly separate ProHo extension, or in a
   different module's API? The contract itself should stay usable without them.
7. **Grant and role.** A new role on a membership, or a separate grant that isn't a
   membership at all? **[accountant]**: does the *revisor fiscal*'s legal role need
   more than ledger scope (minutes, contracts, supporting documents)?
