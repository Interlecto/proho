-- Joining a community is by invitation (domain-model.md §3). An invitation isn't a
-- membership row, because the invitee may not have an account yet and
-- membership.account_id is required. Only a hash of its token is stored.

create table invitation (
    id            uuid primary key,
    community_id  uuid         not null references community (id),
    email         varchar(320) not null check (email = lower(email)),
    person_id     uuid,
    token_hash    text         not null unique,
    invited_by    uuid         not null,
    created_at    timestamptz  not null default now(),
    expires_at    timestamptz  not null,
    accepted_at   timestamptz,
    revoked_at    timestamptz,
    membership_id uuid,
    foreign key (person_id, community_id) references person (id, community_id),
    foreign key (invited_by, community_id) references membership (id, community_id),
    foreign key (membership_id, community_id) references membership (id, community_id)
);

-- One open invitation per email in a community. An expired one is revoked when
-- the email is invited again.
create unique index invitation_open_email_idx
    on invitation (community_id, email)
    where accepted_at is null and revoked_at is null;

alter table invitation enable row level security;
create policy invitation_tenant on invitation
    using (community_id = app_current_community())
    with check (community_id = app_current_community());

grant select, insert, update on invitation to proho_app;

-- Accepting starts with no community bound, so row-level security hides the row.
-- This function runs as the table's owner to get past that, and returns only the
-- usable invitation matching a hash, which can't be guessed. It can't list anything.

create function app_find_invitation(p_token_hash text)
    returns table (
        id             uuid,
        community_id   uuid,
        community_name varchar,
        email          varchar,
        person_id      uuid
    )
    language sql stable security definer
    set search_path = public
    as $$
        select i.id, i.community_id, c.name, i.email, i.person_id
          from invitation i
          join community c on c.id = i.community_id
         where i.token_hash = p_token_hash
           and i.accepted_at is null
           and i.revoked_at is null
           and i.expires_at > now()
    $$;

revoke all on function app_find_invitation(text) from public;
grant execute on function app_find_invitation(text) to proho_app;

-- A role can only be held once at a time.
create unique index role_assignment_current_idx
    on role_assignment (membership_id, role)
    where valid_to is null;
