# Database migrations

Flyway migrations (`V<n>__<description>.sql`). Applied by the backend on
startup and in CI, as the role that owns the tables, not the role the
application connects as.

A new table holding community data needs a `community_id`, composite foreign
keys, row-level security and explicit grants: see
[docs/security/tenant-isolation.md](../docs/security/tenant-isolation.md#adding-a-table).
