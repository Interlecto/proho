package net.interlecto.proho.audit

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AuditEventRepository : CrudRepository<AuditEvent, UUID> {
    fun findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
        entityType: String,
        entityId: UUID,
    ): List<AuditEvent>
}
