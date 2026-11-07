package net.interlecto.proho

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assumptions.assumeThat
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder

class DatabaseConfigurationTest {
    @Test
    fun `the application won't start without its database settings`() {
        assumeThat(System.getenv("PROHO_DB_URL")).isNull()

        assertThatThrownBy {
            SpringApplicationBuilder(ProHoApplication::class.java).web(WebApplicationType.NONE).run().close()
        }.hasStackTraceContaining("PROHO_DB_URL")
    }
}
