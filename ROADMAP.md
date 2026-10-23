# Revised roadmap: Residential community management platform

The roadmap should now treat residential communities as the initial product domain, while keeping the architecture reusable for clubs, gated communities, and other small organizations.

The most important change is the accounting requirement: the MVP will already include a double-entry ledger, not merely accounting-ready exports. Electronic payment integration and federated identity belong to the first release after the MVP, while the MVP supports manually recorded payments.

The platform will have three clients sharing one centralized backend: web, Android, and native iOS.

## 1. Confirmed product decisions

| Area              | MVP                                         | First release                                                  |
| ----------------- | ------------------------------------------- | -------------------------------------------------------------- |
| Target market     | Residential communities                     | Expand to additional organization types                        |
| Platform clients  | Web + native Android + native iOS           | Continued feature parity                                       |
| Backend           | Kotlin API                                  | Same API, extended integrations                                |
| Web               | React + TypeScript                          | Full administrative interface                                  |
| Android           | Kotlin + Jetpack Compose                    | Extended mobile administration                                 |
| iOS               | Native Swift + SwiftUI                      | Extended mobile administration                                 |
| Organizations     | One account can access multiple communities | Same model                                                     |
| Deployment        | Centralized, multi-tenant platform          | Same model, with scaling as needed                             |
| Authentication    | Email and password                          | Add external identity providers, starting with Google          |
| Fees              | Ordinary and extraordinary assessments      | Same, with expanded automation                                 |
| Accounting        | Double-entry ledger and accounting reports  | Expanded accounting operations                                 |
| Payments          | Manually record payments                    | Electronic payments via provider adapters                      |
| Payment providers | No electronic provider required for MVP     | Colombia PSE, Colombia's Llave system, PayPal, Venmo           |
| Social features   | Announcements, boards, messaging            | Expanded moderation and notifications                          |
| Offline access    | Read cached news and direct messages        | More resilient synchronization and additional offline features |
| Modularity        | Features as modules enabled per community kind and plan | New community kinds (clubs, associations) as templates |
| Plans             | Free plan for small communities (< 20 units and < 60 SMMLV budget), platform-held keys; entitlements model | Paid plans: complete data ownership and customer-held keys as the minimum |

Two distinctions are worth preserving in the implementation plan:

- Native iOS means a Swift/SwiftUI application, not a Kotlin Multiplatform client. Kotlin remains the backend language and Android language.
- Payment-provider support is an integration goal, not a guarantee that every named provider offers the same merchant onboarding, APIs, or transaction capabilities in Colombia. Provider availability and eligibility must be verified before committing to each connector.

## 2. Revised architecture

Community members and administrators

One account · Multiple communities · Role-based access

Web

React + TypeScript

Android

Kotlin + Compose

iOS

Swift + SwiftUI

Kotlin / Spring Boot API

Authentication · Authorization · Business rules · REST/OpenAPI

PostgreSQL

Community data, billing, ledger, audit history

Shared infrastructure

Files, background jobs, push, email, payment adapters

### Architectural decisions

Backend: Kotlin with Spring Boot, PostgreSQL, REST, OpenAPI, and Flyway migrations. Keep a modular monolith initially.

Clients: Three independently deployable applications. Share the API contract, terminology, permission model, and acceptance tests, but use platform-native user interfaces.

Identity: Use a standard OIDC-compatible identity architecture where practical, with email/password as the MVP login method. Add Google sign-in in the first release without making Google a prerequisite for an account.

Multi-tenancy: A user has one platform identity and can hold different memberships and roles in different communities. Community data is centrally hosted and isolated by organization-scoped authorization.

Offline data: Each mobile application maintains a local database for selected cached content. The server remains authoritative; offline access does not grant additional permissions or bypass content deletion and access restrictions when synchronization resumes.

Accounting: The ledger is a first-class backend module in the MVP. Billing, payments, expenses, and journal entries must have explicit relationships and auditable transaction histories.

## 3. Revised functional scope

### 3.1. Residential community management

The initial domain model should represent the realities of apartment buildings and gated communities rather than assuming every organization is a generic membership club.

Include:

- Communities, buildings, blocks, floors, and residential units.
- Owners, tenants, residents, household members, and authorized representatives.
- Unit occupancy and residency history.
- Membership and access rights independent of property ownership.
- Community administrators, treasurers, board members, and moderators.
- Community rules, official documents, meeting minutes, and notices.
- Maintenance requests and common-area issues, initially as a later-phase feature.
- Optional common facilities, reservations, and visitor management as future extensions.

A residential unit should not be synonymous with a person or a membership. A unit can have several residents, ownership can change, and a person can be associated with more than one unit or community.

### 3.2. Billing and double-entry accounting in the MVP

The MVP must include the following accounting foundation:

- Configurable chart of accounts.
- Accounting periods and fiscal-year settings.
- Double-entry journal entries and journal lines.
- Ordinary and extraordinary assessments, allocated per unit or by ownership coefficient (*coeficiente de copropiedad*) from the start.
- Accounts receivable by community and residential unit.
- Manual payment recording and allocation.
- Resident-reported payments (*reportar pago*): a resident reports a transfer or deposit with its reference and receipt; it stays pending, without touching the ledger, until a treasurer confirms or rejects it, and only confirmation records and allocates the payment.
- Income and expense classification.
- Expense recording, including suppliers and supporting documents where required.
- Trial balance, general ledger, income and expense statement, and balance sheet.
- Member statements, collection reports, and aging reports.
- Opening balances and controlled correction/reversal procedures.
- Period-end controls and audit history.

The accounting design should distinguish the legal owner or responsible party, the residential unit, and the person making a payment. These can be different entities.

A possible minimum transaction flow:

1. Generate an assessment for a unit or responsible party.
2. Post the assessment and create the corresponding accounting entry.
3. Record a payment manually, including date, amount, method, and reference.
4. Allocate the payment to outstanding assessments.
5. Post the cash or bank debit and receivable credit.
6. Generate updated member statements and accounting reports.
7. Correct errors through reversals or adjustments, preserving the original records.

For example, posting a 100-unit assessment might debit accounts receivable and credit assessment income. Recording a payment would debit cash or bank and credit accounts receivable. The actual chart of accounts and recognition policies must be configurable to match the applicable accounting rules.

The ledger must also handle payments in advance, credits, unallocated receipts, expenses paid by administrators and later reimbursed, and transactions spanning accounting periods. These are not good candidates for ad hoc balance adjustments.

### 3.3. Payment integrations in the first release

Implement an internal payment-provider adapter interface so that billing and accounting logic do not depend on a particular provider's API.

The initial connector backlog is:

| Connector             | Integration objective                                                                                  |
| --------------------- | ------------------------------------------------------------------------------------------------------ |
| Colombia PSE          | Initiate payments through a supported PSE payment provider or gateway                                  |
| Colombia Llave system | Integrate with the applicable Colombian instant-payment network and a supported participant or gateway |
| PayPal                | Support eligible PayPal checkout/payment flows                                                         |
| Venmo                 | Support eligible Venmo payment flows, subject to merchant and geographic availability                  |

The adapter should normalize provider-specific events into common platform concepts: payment initiated, pending, confirmed, failed, cancelled, refunded, and disputed where applicable.

Required safeguards include:

- Unique internal payment and provider transaction identifiers.
- Idempotent payment creation and event processing.
- Verified webhook signatures or equivalent authentication.
- Server-side confirmation of successful payments.
- Reconciliation against provider settlement or transaction reports.
- Separate tracking of payment status, settlement status, fees, and refunds.
- No marking an assessment as paid solely because a client reports a successful redirect.
- Secure handling of provider credentials and secrets.

Provider discovery, account eligibility, API access, merchant onboarding, and settlement currency must be validated before implementation. In particular, the Llave requirement should be tied to the specific Colombian payment-network interface intended, and Venmo's availability must not be assumed for Colombian community accounts.

Manual payments must remain supported after electronic payments are introduced. Bank transfers, cash, corrections, and exceptional transactions will continue to need manual workflows.

### 3.5. Publishing workflow

Announcements, news, and board posts follow the publication model of the original ProHo design (see `docs/legacy-salvage.md`):

- States: draft, pending approval, pending publication, published, and withdrawn.
- Audience on publication: community members, privileged (board and administration), or public.
- Optional scheduled publication and expiry dates.
- Edit history with editor and changes, feeding the audit log.
- Categories, tags, and Markdown bodies.

Unmoderated boards skip the approval state; moderated boards and official announcements require it.

### 3.6. Backlog from the original design

These features were part of the 2016 ProHo design and are kept as candidates after the first release, in addition to maintenance requests, reservations, and visitor management:

- Community marketplace (*tienda*): units offered for rent or sale, and services offered by residents.
- Public visitor page with content the community chooses to publish, such as available units.
- Photo galleries.
- Document repository for rules, minutes, and official documents.
- Search across permitted content.
- Threaded comments on posts and announcements.

### 3.4. Offline functionality

Offline access is an explicit requirement for the mobile clients, but the MVP can keep it narrowly scoped.

MVP offline scope

- Read previously synchronized direct messages.
- Read previously synchronized announcements and news.
- Browse selected cached community information.
- Display the last synchronization time.
- Clearly distinguish cached information from current server state.

First-release improvements

- Incremental synchronization rather than repeatedly downloading entire histories.
- Persistent local storage and a synchronization cursor.
- Conflict handling for editable data.
- Retry logic and recovery after app termination or network loss.
- Cache expiry and local data deletion after logout or loss of access.
- Push-triggered refresh when the device is online.

The first offline implementation should be read-only for messages and announcements. Sending messages, recording payments, changing balances, and approving financial operations should require a connection unless a later design explicitly defines safe queuing and conflict-resolution rules.

## 4. Updated implementation roadmap

The previous 20-week schedule should be treated as an initial planning estimate, not a commitment. Native iOS, a real double-entry ledger, and offline synchronization materially increase the scope. The following sequence separates the accounting foundation from electronic payment integrations.

## 0

Domain definition and technical foundation

Weeks 1–3 · Indicative

Residential domain model, accounting policies, API contract, repository, CI/CD, security architecture, and initial UI prototypes.

## 1

Identity, communities, and three client foundations

Weeks 4–8

Email/password authentication, community switching, units, residents, roles, member directory, and basic navigation on web, Android, and iOS.

## 2

Billing and accounting MVP

Weeks 9–16

Assessment generation, receivables, manual payments, double-entry ledger, expenses, financial reports, and reconciliation.

## 3

Community communication and offline reading

Weeks 13–18 · May overlap with Phase 2

Announcements, moderated and unmoderated boards, direct messaging, notifications, and mobile caching.

## 4

MVP integration, pilot, and hardening

Weeks 19–22 · Indicative

Data import, opening balances, end-to-end tests, security validation, ledger reconciliation, and pilot deployment.

## 5

First release: integrations and advanced features

After MVP acceptance

Electronic payment connectors, Google sign-in, improved offline synchronization, and expanded administrative workflows.

The phases can overlap where dependencies permit. Accounting and financial correctness should not be compressed simply to meet a calendar target. The actual schedule depends on team size, experience, provider onboarding, and the scope of iOS work.

### Phase 0 — Foundation

- Define residential-community entities and relationships.
- Establish accounting policies and chart-of-accounts configuration.
- Document the journal posting rules for assessments, payments, expenses, credits, and reversals.
- Establish the shared REST/OpenAPI contract.
- Create the Kotlin/Spring Boot backend and PostgreSQL schema.
- Set up React/TypeScript, Kotlin/Compose, and Swift/SwiftUI projects.
- Establish automated builds, tests, migrations, and deployment.
- Define tenant isolation, audit logging, and secrets management.

Exit criterion: The team can deploy the backend and all three client shells, with a reviewed domain model and documented accounting invariants.

### Phase 1 — Identity and community administration

- Email/password authentication and account recovery.
- One account associated with multiple communities.
- Community, building, unit, and resident management.
- Owner, tenant, resident, and representative relationships.
- Membership and role administration.
- Member profiles and privacy controls.
- Consistent navigation and core member dashboard on all three platforms.

Exit criterion: A resident can switch between communities and view permitted information without duplicating accounts or exposing another community's data.

### Phase 2 — Accounting MVP

- Chart of accounts and accounting-period configuration.
- Ordinary recurring and extraordinary assessments.
- Posting engine with balanced journal entries.
- Receivables by responsible party and residential unit.
- Manual payment entry, allocation, and receipt generation.
- Resident-reported payments with treasurer confirmation.
- Expense and supplier records.
- Credits, refunds, reversals, and opening balances.
- Trial balance, general ledger, income and expense statement, and balance sheet.
- Collection, aging, and member-statement reports.
- CSV/XLSX exports and relevant PDF reports.
- Reconciliation and period-close controls.

Exit criterion: Financial reports reconcile to the ledger, every posted journal entry balances, and corrections preserve a complete audit trail.

### Phase 3 — Social features and offline reading

- Official announcements and distribution lists.
- Moderated and unmoderated discussion boards, using the publishing workflow in §3.5.
- Direct messaging and conversation history.
- In-app and email notifications.
- Android push notifications and iOS push notifications.
- Local caching of news and direct messages on both mobile platforms.
- Cache expiry, logout cleanup, and synchronization recovery.
- Member reporting and moderation tools.

Exit criterion: Members can use core communication features from any client and read previously synchronized messages and news when offline on mobile.

### Phase 4 — Pilot and production readiness

- Import members, units, and opening financial balances.
- Validate financial totals against independently prepared records.
- Test authorization and cross-tenant isolation.
- Verify duplicate payment prevention and transaction rollback.
- Test offline behavior and synchronization after reconnecting.
- Test database backups and restoration.
- Establish monitoring, incident procedures, and operational documentation.
- Pilot with one or more residential communities.

Exit criterion: The platform can operate a pilot community without manual database repairs, and financial records can be independently reconciled.

### Phase 5 — First release

- Add Google sign-in through a standard identity-provider integration.
- Introduce the payment-provider adapter interface.
- Implement the first available electronic payment connector.
- Add the remaining connectors according to verified provider eligibility and API availability.
- Implement provider webhooks, refunds, reconciliation, and settlement reporting.
- Improve incremental offline synchronization and conflict handling.
- Expand mobile administrative functions.
- Add further accounting and residential-management workflows based on pilot feedback.

Exit criterion: Electronic payments can be initiated, verified, recorded in the ledger, and reconciled with provider records without bypassing accounting controls.

## 5. Repository structure

A monorepo remains appropriate for this product, although the three clients have separate build systems and release processes.

```
community-platform/
├── backend/
│   ├── src/
│   │   ├── identity/
│   │   ├── communities/
│   │   ├── properties/
│   │   ├── residents/
│   │   ├── billing/
│   │   ├── accounting/
│   │   ├── payments/
│   │   ├── communications/
│   │   ├── notifications/
│   │   └── audit/
│   └── integration-tests/
├── web/
│   ├── src/
│   └── tests/
├── android/
│   ├── app/
│   ├── core/
│   ├── feature/
│   └── tests/
├── ios/
│   ├── CommunityApp/
│   ├── Features/
│   ├── Core/
│   └── Tests/
├── api/
│   └── openapi/
├── database/
│   └── migrations/
├── infrastructure/
│   ├── docker/
│   └── deployment/
└── docs/
    ├── architecture/
    ├── accounting/
    ├── security/
    ├── api/
    └── operations/
```

Keep the backend modular, with explicit boundaries between billing, payments, and accounting. A payment connector should not directly modify journal tables; it should submit a verified financial event through the relevant application service, which applies the accounting rules transactionally.

## 6. Revised release acceptance criteria

Before declaring the MVP complete, verify the following:

- A user can belong to multiple communities and have different permissions in each.
- Residential units, occupants, owners, and responsible parties are modeled independently.
- Ordinary and extraordinary assessments can be generated and posted.
- Manual payments can be allocated to one or more outstanding assessments.
- The accounting ledger maintains balanced journal entries.
- Trial balance and financial statements reconcile to the ledger.
- Financial corrections do not erase original transactions.
- Members can access their basic information and communications on web, Android, and iOS.
- Android and iOS can display previously synchronized news and direct messages offline.
- Authorization, audit logging, backups, and restoration have been tested.
- Financial and privacy-related acceptance tests pass before pilot deployment.

## 7. Remaining design decisions

The broad product direction is now defined. A few implementation details still need explicit decisions before development reaches the relevant modules:

1. Accounting jurisdiction: Whether the first community operates under Colombian accounting and reporting requirements, and which reporting conventions the accountant expects.
2. Assessment rules: Whether fees are charged per unit, by area, by coefficient, by occupancy, or through a combination of methods. The data model should support configurable allocation rules. Allocation by ownership coefficient is already required: the original ProHo design splits extraordinary assessments that way.
3. Payment connector order: Which provider should be integrated first, based on merchant eligibility, transaction fees, API maturity, and settlement behavior.
4. Offline retention: How much message history to cache, for how long, and how sensitive local data should be protected on shared or lost devices.
5. iOS distribution: Whether the first iOS build will be distributed through TestFlight and the App Store, with a corresponding Apple Developer account and signing pipeline.

The most consequential next step is to define the residential domain model and accounting posting rules together. That prevents the fee-management module from evolving into a standalone receivables system that later needs to be retrofitted into the general ledger.
