package net.interlecto.proho

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres as Zonky
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.test.context.DynamicPropertyRegistry

/**
 * One PostgreSQL server per test JVM, so integration tests run without Docker
 * on developer machines and in CI alike. It has the same roles as a real
 * deployment: migrations run as the non-superuser owner, and the application
 * connects as proho_app. Connecting as the server's superuser would make tests
 * pass with row-level security doing nothing.
 */
object EmbeddedPostgres {
    private const val DATABASE = "proho"

    private val server: Zonky by lazy {
        Zonky.builder().start().also { server ->
            val superuser = JdbcClient.create(server.getDatabase("postgres", "postgres"))
            superuser.sql("create role proho login password 'proho'").update()
            superuser.sql("create role proho_app login password 'proho_app' nosuperuser nobypassrls").update()
            superuser.sql("create database $DATABASE owner proho").update()
        }
    }

    fun register(registry: DynamicPropertyRegistry) {
        // Not server.getJdbcUrl(): it puts a user in the URL, which overrides the
        // username settings below and would connect the application as the owner.
        val url = "jdbc:postgresql://localhost:${server.port}/$DATABASE"
        registry.add("spring.datasource.url") { url }
        registry.add("spring.datasource.username") { "proho_app" }
        registry.add("spring.datasource.password") { "proho_app" }
        registry.add("spring.flyway.url") { url }
        registry.add("spring.flyway.user") { "proho" }
        registry.add("spring.flyway.password") { "proho" }
    }

    // One shared connection: opening a new one per statement takes most of a
    // test's time on Windows.
    private val superuserJdbc: JdbcTemplate by lazy {
        JdbcTemplate(SingleConnectionDataSource(server.getDatabase("postgres", DATABASE).connection, true))
    }

    /** Sees everything, so tests can check what RLS hid from the application. */
    fun superuser(): JdbcTemplate = superuserJdbc

    /** The role that ran the migrations and owns the tables. */
    fun owner(): JdbcTemplate = JdbcTemplate(server.getDatabase("proho", DATABASE))
}
