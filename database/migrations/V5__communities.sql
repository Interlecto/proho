-- Communities: tenants, memberships, assigned roles. See
-- docs/architecture/domain-model.md §3 ("Membership", "Roles") and §4
-- ("Community"). Coefficients (§4 "Coefficients", invariant #3) and the
-- `plans` module's entitlements (§10.3) are deliberately out of scope: no
-- billing/accounting consumer exists yet to need either. `plan` stays a
-- plain string column here, not an effective-dated table, until one does.

alter table community
    add column legal_name varchar(200),
    add column tax_id     varchar(32),
    add column plan       varchar(40),
    add column address    text;

create table membership (
    id           uuid primary key,
    account_id   uuid        not null references account (id),
    community_id uuid        not null references community (id),
    person_id    uuid,
    status       varchar(20) not null
                 check (status in ('invited', 'active', 'suspended', 'ended')),
    invited_by   uuid        references account (id),
    invited_at   timestamptz,
    joined_at    timestamptz,
    ended_at     timestamptz,
    created_at   timestamptz not null default now()
);

-- Invariant #2: at most one non-ended membership per (account, community).
create unique index membership_account_community_active_idx
    on membership (account_id, community_id) where status <> 'ended';

create index membership_community_idx on membership (community_id);

-- person_id gets a foreign key once residents.person exists (V7), same
-- forward-reference precedent as V3__audit.sql's actor_membership_id.

create table role_assignment (
    id            uuid primary key,
    membership_id uuid        not null references membership (id),
    role          varchar(30) not null
                  check (role in ('administrator', 'accountant', 'treasurer',
                                   'auditor', 'council_member', 'moderator', 'staff')),
    valid_from    date        not null,
    valid_to      date,
    created_at    timestamptz not null default now()
);

create index role_assignment_membership_idx on role_assignment (membership_id);
