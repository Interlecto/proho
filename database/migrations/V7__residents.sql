-- Residents: people and unit relationships. See domain-model.md §5.
-- emails/phones/privacy are deferred to the privacy-controls ROADMAP bullet
-- (Phase 1, separate from this slice) -- not stored at all yet, so there is
-- no half-built per-field visibility to get wrong.
--
-- Also closes the forward references V3__audit.sql flagged: membership and
-- community tables now exist, so audit_event.actor_membership_id and
-- audit_event.community_id get their foreign keys.

create table person (
    id           uuid primary key,
    community_id uuid         not null references community (id),
    kind         varchar(10)  not null check (kind in ('natural', 'legal')),
    given_name   varchar(120),
    surname      varchar(120),
    legal_name   varchar(200),
    id_type      varchar(10)  check (id_type in ('CC', 'CE', 'NIT', 'PA', 'TI')),
    id_number    varchar(40),
    created_at   timestamptz  not null default now()
);

create index person_community_idx on person (community_id);

-- Unique per community only when present (NIT/CC etc. are optional).
create unique index person_community_id_number_idx
    on person (community_id, id_type, id_number)
    where id_number is not null;

alter table membership
    add constraint membership_person_fk foreign key (person_id) references person (id);

create table unit_relationship (
    id         uuid primary key,
    unit_id    uuid        not null references unit (id),
    person_id  uuid        not null references person (id),
    role       varchar(20) not null
               check (role in ('owner', 'tenant', 'resident', 'household_member', 'representative')),
    share      numeric(7, 4),
    valid_from date        not null,
    valid_to   date,
    created_at timestamptz not null default now()
);

create index unit_relationship_unit_idx on unit_relationship (unit_id);
create index unit_relationship_person_idx on unit_relationship (person_id);

alter table audit_event
    add constraint audit_event_membership_fk foreign key (actor_membership_id) references membership (id),
    add constraint audit_event_community_fk foreign key (community_id) references community (id);
