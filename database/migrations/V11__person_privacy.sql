-- Contact details and who can see a person's record. See
-- docs/security/privacy.md. Everything starts at 'privileged', the most private
-- setting: other members see a person only if that person opts in.
--
-- 'privileged' can't exclude administration, which needs these details to run the
-- community. 'public' from domain-model.md §3 is left out: no public page exists.

create domain person_visibility as varchar(20)
    check (value in ('privileged', 'community'));

alter table person
    add column email             varchar(320),
    add column phone             varchar(40),
    add column name_visibility   person_visibility not null default 'privileged',
    add column email_visibility  person_visibility not null default 'privileged',
    add column phone_visibility  person_visibility not null default 'privileged',
    add column units_visibility  person_visibility not null default 'privileged';

-- Linking a membership gives its account the person's whole record, so a person
-- can belong to one account at a time. Fails, deliberately, if a database already
-- holds such a pair: nothing in the application has been able to create one.
create unique index membership_person_active_idx
    on membership (person_id)
    where person_id is not null and status <> 'ended';
