package net.interlecto.proho.audit

import org.springframework.stereotype.Service
import java.util.UUID

/**
 * How other modules read the log. It only ever returns one community's events, so
 * platform events (login, registration), which row-level security lets any bound
 * community read, stay out of a community's view.
 */
@Service
class AuditEventReader(
    private val events: AuditEventRepository,
) {
    /** Newest first. [before] is the id of the last event of the previous page. */
    fun forCommunity(
        communityId: UUID,
        before: UUID?,
        limit: Int,
    ): List<AuditEvent> =
        if (before == null) {
            events.findCommunityPage(communityId, limit)
        } else {
            events.findCommunityPageBefore(communityId, before, limit)
        }
}
