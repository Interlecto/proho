package net.interlecto.proho

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres as Zonky
import org.springframework.test.context.DynamicPropertyRegistry

/**
 * One PostgreSQL server per test JVM, so integration tests run without Docker
 * on developer machines and in CI alike.
 */
object EmbeddedPostgres {
    private val server: Zonky by lazy { Zonky.builder().start() }

    fun register(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { server.getJdbcUrl("postgres", "postgres") }
        registry.add("spring.datasource.username") { "postgres" }
        registry.add("spring.datasource.password") { "postgres" }
    }
}
