-- Baseline: the tenant table every other table will reference.
-- Entities follow docs/architecture/domain-model.md.

create table community (
    id          uuid primary key,
    key         varchar(64)  not null unique,
    name        varchar(200) not null,
    kind        varchar(40)  not null,
    country     char(2)      not null default 'CO',
    currency    char(3)      not null default 'COP',
    timezone    varchar(64)  not null default 'America/Bogota',
    created_at  timestamptz  not null default now()
);
