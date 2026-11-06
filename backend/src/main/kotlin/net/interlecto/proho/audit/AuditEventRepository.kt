package net.interlecto.proho.audit

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AuditEventRepository : CrudRepository<AuditEvent, UUID> {
    fun findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
        entityType: String,
        entityId: UUID,
    ): List<AuditEvent>

    @Query("select * from audit_event where community_id = :communityId order by id desc limit :limit")
    fun findCommunityPage(
        communityId: UUID,
        limit: Int,
    ): List<AuditEvent>

    @Query("select * from audit_event where community_id = :communityId and id < :before order by id desc limit :limit")
    fun findCommunityPageBefore(
        communityId: UUID,
        before: UUID,
        limit: Int,
    ): List<AuditEvent>
}
