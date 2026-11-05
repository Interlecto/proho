-- Identity: bearer sessions and password-reset tokens. See
-- docs/architecture/domain-model.md §3 ("Session/Token"). Only the SHA-256
-- hash of a token is ever stored; the raw token exists only in the
-- response (login) or notification (password reset) that first issues it.

create table account_session (
    id          uuid primary key,
    account_id  uuid        not null references account (id),
    token_hash  text        not null unique,
    created_at  timestamptz not null default now(),
    expires_at  timestamptz not null,
    revoked_at  timestamptz
);

create index account_session_account_idx on account_session (account_id);

create table password_reset_token (
    id          uuid primary key,
    account_id  uuid        not null references account (id),
    token_hash  text        not null unique,
    created_at  timestamptz not null default now(),
    expires_at  timestamptz not null,
    used_at     timestamptz
);

create index password_reset_token_account_idx on password_reset_token (account_id);
