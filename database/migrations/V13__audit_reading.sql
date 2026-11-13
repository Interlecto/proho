-- The community's audit log is read newest first, one page at a time. Ids are
-- UUIDv7, so they sort by time and give a stable place to continue from.

create index audit_event_community_id_idx on audit_event (community_id, id desc);
