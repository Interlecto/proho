package net.interlecto.proho.audit

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.shared.Uuid7
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
class AuditEventRecorderIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var recorder: AuditEventRecorder

    @Autowired
    lateinit var events: AuditEventRepository

    @Test
    fun `records an event with a generated id and timestamp`() {
        val entityId = Uuid7.generate()

        val recorded =
            recorder.record(
                entityType = "account",
                entityId = entityId,
                action = "created",
                summary = "registered",
            )

        assertThat(recorded.id).isNotNull()
        assertThat(recorded.occurredAt).isNotNull()

        val stored = events.findByEntityTypeAndEntityIdOrderByOccurredAtDesc("account", entityId)
        assertThat(stored).hasSize(1)
        assertThat(stored.single().action).isEqualTo("created")
        assertThat(stored.single().summary).isEqualTo("registered")
    }

    @Test
    fun `orders events for the same entity most recent first`() {
        val entityId = Uuid7.generate()

        recorder.record(entityType = "account", entityId = entityId, action = "created")
        Thread.sleep(2)
        recorder.record(entityType = "account", entityId = entityId, action = "locked")

        val stored = events.findByEntityTypeAndEntityIdOrderByOccurredAtDesc("account", entityId)

        assertThat(stored.map { it.action }).containsExactly("locked", "created")
    }
}
