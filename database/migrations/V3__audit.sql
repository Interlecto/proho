-- Audit: one event per command that changes state. See
-- docs/architecture/domain-model.md §8. actor_membership_id and
-- community_id get foreign keys once the communities module's tables exist;
-- they are plain uuid columns for now.

create table audit_event (
    id                  uuid primary key,
    actor_account_id    uuid references account (id),
    actor_membership_id uuid,
    community_id        uuid,
    entity_type         varchar(60) not null,
    entity_id           uuid        not null,
    action              varchar(60) not null,
    summary             text,
    occurred_at         timestamptz not null default now(),
    request_id          varchar(64)
);

create index audit_event_entity_idx on audit_event (entity_type, entity_id);
