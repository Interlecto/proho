package net.interlecto.proho.shared

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Stops the application rather than letting row-level security silently do
 * nothing. Connecting as a superuser or as the tables' owner bypasses every
 * policy with no error, and a new tenant table is unprotected until a
 * migration enables RLS on it. Runs after Flyway, so it sees the final schema,
 * and on every start, so the whole test suite covers future migrations too.
 */
@Component
class TenantIsolationGuard(
    private val jdbcTemplate: JdbcTemplate,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) = verify(jdbcTemplate)

    companion object {
        fun verify(jdbc: JdbcTemplate) {
            val bypassesRls =
                jdbc.queryForObject(
                    "select rolsuper or rolbypassrls from pg_roles where rolname = current_user",
                    Boolean::class.java,
                )
            check(bypassesRls == false) {
                "The application's database role is a superuser or has BYPASSRLS, so row-level security " +
                    "would not apply to it. Connect as proho_app (backend/README.md)."
            }

            val ownedProtectedTables =
                jdbc.queryForList(
                    """
                    select c.relname
                      from pg_class c
                      join pg_namespace n on n.oid = c.relnamespace
                     where n.nspname = current_schema()
                       and c.relkind = 'r'
                       and c.relrowsecurity
                       and not c.relforcerowsecurity
                       and pg_get_userbyid(c.relowner) = current_user
                    """.trimIndent(),
                    String::class.java,
                )
            check(ownedProtectedTables.isEmpty()) {
                "The application's database role owns $ownedProtectedTables, so row-level security would not " +
                    "apply to it. Run migrations as a separate role and connect as proho_app (backend/README.md)."
            }

            val unprotectedTenantTables =
                jdbc.queryForList(
                    """
                    select c.relname
                      from pg_class c
                      join pg_namespace n on n.oid = c.relnamespace
                      join pg_attribute a on a.attrelid = c.oid
                     where n.nspname = current_schema()
                       and c.relkind = 'r'
                       and a.attname = 'community_id'
                       and not a.attisdropped
                       and not c.relrowsecurity
                    """.trimIndent(),
                    String::class.java,
                )
            check(unprotectedTenantTables.isEmpty()) {
                "Tables with a community_id but no row-level security: $unprotectedTenantTables. " +
                    "See docs/security/tenant-isolation.md."
            }
        }
    }
}
