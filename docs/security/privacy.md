# Privacy of people's records

A person's record belongs to the community, but who else can read it is the
person's choice. Other members see nobody by default. This is separate from
[tenant isolation](tenant-isolation.md), which keeps communities apart: these
rules apply inside one community.

Status: **provisional.** The defaults below are a product decision made to be
safe, and they need legal review under Ley 1581 de 2012 before launch.

## Who sees what

| Field | The person | Administrator, accountant, auditor | Treasurer, council member | Someone on the same unit | Any other member |
| ----- | ---------- | ---------------------------------- | ------------------------- | ------------------------ | ---------------- |
| Name | ✓ | ✓ | ✓ | ✓ | If shared |
| Identity document | ✓ | ✓ | | | |
| Email, phone | ✓ | ✓ | ✓ | If shared, with the name | If shared, with the name |
| Units today | ✓ | ✓ | ✓ | That unit only | If shared, with the name |
| Past units, owner shares | ✓ | ✓ | ✓ | | |
| Archived records | ✓ | ✓ | ✓ | | |

Moderators and staff count as other members until a feature needs more. Reading
an archived record by id follows this table, but only administrators can list
archived records.

"Someone on the same unit" means both have a relationship in force today on it,
so an owner can see who their tenant is. It stops when either relationship ends.

A record a member can't see returns the same `404 not_found_in_community` as one
that doesn't exist, so nobody can test whether a person is in the community. A
unit's relationships leave out the ones the caller can't see.

## Choosing

Each person has four settings, all starting at `privileged`, and changes apply
to the next request:

| Setting | `community` means |
| ------- | ----------------- |
| `nameVisibility` | Other members can see this person is in the community, by name |
| `emailVisibility`, `phoneVisibility` | They can also see that detail |
| `unitsVisibility` | They can also see which units this person is on today |

The last three have no effect unless the name is shared too. `privileged` can't
exclude administration, which needs these details to run the community.

- **Only the person changes these,** through `PUT /communities/{id}/profile`. An
  administrator can't widen them, even when correcting the rest of the record.
- **A person with no account stays private,** because nobody can choose for them.
  That includes every legal person (a company that owns a unit) until an
  account is linked to it.
- **A person changes their email, phone and settings.** Their name and identity
  document are the community's legal record, so only an administrator edits those.

## Linking an account to a person

An administrator links a membership to a person with
`PUT /communities/{id}/memberships/{membershipId}/person`. This gives that account
the person's whole record, including the identity document, so:

- Only an administrator can do it, and it's recorded in the audit log.
- A person can be linked to one current membership at a time.
- A linked person can't be archived, and unlinking takes the access away at once.

The administrator has to be sure they're linking the right account, and an
invitation can do it up front.

## Invitations

Only a hash of the token is stored, and the token is shown once to the
administrator, who shares it until email delivery exists. Accepting needs the
token and an account whose email matches, so a forwarded link is useless to anyone
else. It works once and expires after 7 days. The address bar carries it in the
fragment, which browsers don't send to servers, and the page removes it at once.

Account emails aren't verified, so this doesn't yet prove the person controls the
mailbox: someone holding the link could register that email themselves. The
administrator is trusting whoever they send it to. Sending the link by email
closes this for invitations.

## Adding code that reads people

Read people through `ResidentService`, or apply `PersonViewer` directly. Any new
endpoint, report, export or module returning a person's data has to apply these
rules, and `PrivacyIntegrationTest` only covers the endpoints that exist today.
Audit entries must not contain names, documents or contact details.

## Limits

- **Not encrypted:** names, documents, emails and phones are plain text in the
  database, backups and any database tool. The pseudonymization in
  `domain-model.md` §10.4 and invariant 14 is not built.
- **Not built under Ley 1581:** recording consent and its date, a privacy notice,
  exporting a person's data for them, and anonymizing it on request.
- **No read log:** the audit log records changes, not who read a record.
- **One email and one phone:** the domain model describes several of each.
- **Unit details aren't private:** any member can read every unit's number, kind,
  floor, area and *matrícula inmobiliaria*.
- **Enforced in the application:** unlike tenant isolation, no database rule backs
  these checks, so a forgotten check exposes the data. Row-level security works
  per community, not per field.
