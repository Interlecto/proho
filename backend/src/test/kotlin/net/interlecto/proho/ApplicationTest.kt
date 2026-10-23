package net.interlecto.proho

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ApplicationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var jdbc: JdbcClient

    @Test
    fun `health endpoint reports up`() {
        val body = rest.getForObject("/actuator/health", String::class.java)
        assertThat(body).contains("\"status\":\"UP\"")
    }

    @Test
    fun `migrations create the baseline schema`() {
        val count = jdbc.sql("select count(*) from community").query(Long::class.java).single()
        assertThat(count).isZero()
    }
}
