package net.interlecto.proho.audit

import net.interlecto.proho.shared.REQUEST_ID_KEY
import net.interlecto.proho.shared.Uuid7
import org.slf4j.MDC
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * The one way any module writes an AuditEvent. Other modules depend on this
 * class, never on AuditEventRepository directly, so every event goes
 * through the same id/timestamp assignment.
 */
@Service
class AuditEventRecorder(
    private val jdbcAggregateOperations: JdbcAggregateOperations,
) {
    fun record(
        entityType: String,
        entityId: UUID,
        action: String,
        actorAccountId: UUID? = null,
        actorMembershipId: UUID? = null,
        communityId: UUID? = null,
        summary: String? = null,
        requestId: String? = null,
    ): AuditEvent {
        val event =
            AuditEvent(
                id = Uuid7.generate(),
                actorAccountId = actorAccountId,
                actorMembershipId = actorMembershipId,
                communityId = communityId,
                entityType = entityType,
                entityId = entityId,
                action = action,
                summary = summary,
                occurredAt = Instant.now(),
                // The filter's id, when this runs inside a request.
                requestId = requestId ?: MDC.get(REQUEST_ID_KEY),
            )
        return jdbcAggregateOperations.insert(event)
    }
}
