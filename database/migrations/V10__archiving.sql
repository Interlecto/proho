-- Buildings, units and people are archived rather than deleted: other records,
-- and later the financial history, keep pointing at them, and the application's
-- role has no DELETE privilege. An archived unit keeps its number reserved.
--
-- Relationship periods and owner shares were only checked in the service, or not
-- at all. These fail, deliberately, if a database already holds such a row: it has
-- to be repaired by hand.

alter table building add column archived_at timestamptz;
alter table unit     add column archived_at timestamptz;
alter table person   add column archived_at timestamptz;

alter table unit_relationship
    add constraint unit_relationship_period_check
        check (valid_to is null or valid_to >= valid_from),
    add constraint unit_relationship_share_check
        check (share is null or share between 0 and 100);
