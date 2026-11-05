-- Identity: one login per human, platform-wide. Holds no community data.
-- See docs/architecture/domain-model.md §3.

create table account (
    id             uuid primary key,
    email          varchar(320) not null unique,
    display_name   varchar(200) not null,
    locale         varchar(10)  not null default 'es-CO',
    status         varchar(30)  not null default 'pending_verification'
                   check (status in ('pending_verification', 'active', 'locked', 'closed')),
    password_hash  text         not null,
    created_at     timestamptz  not null default now()
);
