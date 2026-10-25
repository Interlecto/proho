package net.interlecto.proho.audit

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * One record of a command that changed state. See
 * docs/architecture/domain-model.md §8: every such command writes one of
 * these, in the same transaction as the change itself.
 */
@Table("audit_event")
data class AuditEvent(
    @Id val id: UUID,
    val actorAccountId: UUID?,
    val actorMembershipId: UUID?,
    val communityId: UUID?,
    val entityType: String,
    val entityId: UUID,
    val action: String,
    val summary: String?,
    val occurredAt: Instant,
    val requestId: String?,
)
