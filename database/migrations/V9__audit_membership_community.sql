-- An audit event's actor must be a membership of the event's community. V7 only
-- checks that the membership exists, so an event in one community could name
-- another community's member. See docs/security/tenant-isolation.md.
--
-- A composite key isn't checked when any of its columns is null, so the check
-- constraint stops a membership being recorded on an event with no community.
-- These fail, deliberately, if a database already holds such an event: it has
-- to be repaired by hand.

alter table audit_event
    drop constraint audit_event_membership_fk,
    add constraint audit_event_membership_community_fk
        foreign key (actor_membership_id, community_id) references membership (id, community_id),
    add constraint audit_event_membership_needs_community
        check (actor_membership_id is null or community_id is not null);
