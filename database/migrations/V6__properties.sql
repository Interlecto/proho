-- Properties: buildings and units. See domain-model.md §4. Coefficients
-- (CoefficientSchedule/UnitCoefficient) are explicitly deferred: nothing in
-- billing/accounting exists yet to consume them, and invariant #3 is
-- effective-dated logic with no current caller — same reasoning as
-- identity's deferred Credential table. Revisit when billing lands.

create table building (
    id           uuid primary key,
    community_id uuid         not null references community (id),
    name         varchar(200) not null,
    created_at   timestamptz  not null default now()
);

create index building_community_idx on building (community_id);

create table unit (
    id           uuid primary key,
    community_id uuid         not null references community (id),
    building_id  uuid         references building (id),
    number       varchar(40)  not null,
    kind         varchar(20)  not null
                 check (kind in ('apartment', 'house', 'commercial', 'office', 'parking', 'storage')),
    floor        varchar(20),
    area_m2      numeric(10, 2),
    registry_id  varchar(60),
    created_at   timestamptz  not null default now(),
    unique (community_id, number)
);

create index unit_community_idx on unit (community_id);
create index unit_building_idx on unit (building_id);
