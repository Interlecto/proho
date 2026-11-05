-- Tenant isolation at the database level. See docs/security/tenant-isolation.md.
--
-- Two independent backstops behind the service-layer membership checks:
--   1. Composite foreign keys: a row can't reference another community's row,
--      whoever writes it. Row-level security can't do this by itself, because
--      foreign key checks ignore RLS policies.
--   2. Row-level security: the application's role sees and writes only the
--      community bound to its connection, and nothing when none is bound.
--
-- RLS doesn't apply to superusers or to a table's owner, so the application
-- must connect as a separate role. proho_app is provisioned outside migrations
-- so its password never appears here (backend/README.md), and
-- TenantIsolationGuard refuses to start the application on a role that would
-- bypass these policies.

do $$
begin
    if not exists (select from pg_roles where rolname = 'proho_app') then
        raise exception 'Role proho_app does not exist. Create it before migrating: see backend/README.md';
    end if;
end
$$;

-- Tables that reached their community through a parent get their own column,
-- so the policy on them is the same simple comparison as everywhere else and
-- the composite keys below can pin both parents to one community.

alter table role_assignment add column community_id uuid;
update role_assignment ra
   set community_id = m.community_id
  from membership m
 where m.id = ra.membership_id;
alter table role_assignment alter column community_id set not null;

alter table unit_relationship add column community_id uuid;
update unit_relationship ur
   set community_id = u.community_id
  from unit u
 where u.id = ur.unit_id;
alter table unit_relationship alter column community_id set not null;

-- Composite foreign keys. These fail, deliberately, if a database already
-- holds a cross-community reference: such a row has to be repaired by hand.

alter table building   add constraint building_id_community_key   unique (id, community_id);
alter table unit       add constraint unit_id_community_key       unique (id, community_id);
alter table person     add constraint person_id_community_key     unique (id, community_id);
alter table membership add constraint membership_id_community_key unique (id, community_id);

alter table unit
    drop constraint unit_building_id_fkey,
    add constraint unit_building_community_fk
        foreign key (building_id, community_id) references building (id, community_id);

alter table membership
    drop constraint membership_person_fk,
    add constraint membership_person_community_fk
        foreign key (person_id, community_id) references person (id, community_id);

alter table role_assignment
    drop constraint role_assignment_membership_id_fkey,
    add constraint role_assignment_membership_community_fk
        foreign key (membership_id, community_id) references membership (id, community_id);

alter table unit_relationship
    drop constraint unit_relationship_unit_id_fkey,
    drop constraint unit_relationship_person_id_fkey,
    add constraint unit_relationship_unit_community_fk
        foreign key (unit_id, community_id) references unit (id, community_id),
    add constraint unit_relationship_person_community_fk
        foreign key (person_id, community_id) references person (id, community_id);

-- The connection's context, set by TenantAwareDataSource and TenantBinder.
-- An unset or empty value is NULL, and NULL never equals anything, so a
-- connection with no context gets no tenant rows: isolation fails closed.

create function app_current_community() returns uuid
    language sql stable
    as $$ select nullif(current_setting('app.community_id', true), '')::uuid $$;

create function app_current_account() returns uuid
    language sql stable
    as $$ select nullif(current_setting('app.account_id', true), '')::uuid $$;

-- Tables holding only one community's records.

alter table building enable row level security;
create policy building_tenant on building
    using (community_id = app_current_community())
    with check (community_id = app_current_community());

alter table unit enable row level security;
create policy unit_tenant on unit
    using (community_id = app_current_community())
    with check (community_id = app_current_community());

alter table person enable row level security;
create policy person_tenant on person
    using (community_id = app_current_community())
    with check (community_id = app_current_community());

alter table unit_relationship enable row level security;
create policy unit_relationship_tenant on unit_relationship
    using (community_id = app_current_community())
    with check (community_id = app_current_community());

alter table role_assignment enable row level security;
create policy role_assignment_tenant on role_assignment
    using (community_id = app_current_community())
    with check (community_id = app_current_community());

-- Membership is how a request gets a community bound in the first place, so
-- an account can always read its own, in any community. Writes still need the
-- community bound, so nobody can add themselves to a community at this level.

alter table membership enable row level security;
create policy membership_read on membership for select
    using (account_id = app_current_account() or community_id = app_current_community());
create policy membership_insert on membership for insert
    with check (community_id = app_current_community());
create policy membership_update on membership for update
    using (community_id = app_current_community())
    with check (community_id = app_current_community());

-- Community: GET /communities lists an account's communities with none bound.
-- Founding a community binds its new id first (CommunityService.create). The
-- key's global uniqueness still holds, because unique indexes ignore RLS.

alter table community enable row level security;
create policy community_read on community for select
    using (
        id = app_current_community()
        or exists (
            select 1
              from membership m
             where m.community_id = community.id
               and m.account_id = app_current_account()
               and m.status = 'active'
        )
    );
create policy community_insert on community for insert
    with check (id = app_current_community());
create policy community_update on community for update
    using (id = app_current_community())
    with check (id = app_current_community());

-- Audit: platform events (login, registration) have no community and aren't
-- tenant data. The role can't update or delete any event: it gets no such
-- privilege below, which makes the log append-only for the application.

alter table audit_event enable row level security;
create policy audit_event_read on audit_event for select
    using (community_id is null or community_id = app_current_community());
create policy audit_event_insert on audit_event for insert
    with check (community_id is null or community_id = app_current_community());

-- Least privilege. No DELETE anywhere: nothing in the application deletes.
-- account, account_session and password_reset_token stay outside RLS: they
-- belong to a login, not a community, and are read before anyone is
-- authenticated.

grant select, insert, update on
    account, account_session, password_reset_token,
    community, membership, role_assignment,
    building, unit, person, unit_relationship
    to proho_app;
grant select, insert on audit_event to proho_app;
