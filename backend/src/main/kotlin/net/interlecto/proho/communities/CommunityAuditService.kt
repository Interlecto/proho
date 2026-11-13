package net.interlecto.proho.communities

import net.interlecto.proho.audit.AuditEvent
import net.interlecto.proho.audit.AuditEventReader
import net.interlecto.proho.identity.AccountRepository
import org.springframework.stereotype.Service
import java.util.UUID

private val AUDIT_READERS = setOf(Role.ADMINISTRATOR, Role.AUDITOR)
private const val DEFAULT_PAGE = 50
private const val MAX_PAGE = 200

data class AuditEntry(val event: AuditEvent, val actorName: String?)

/** A community's own audit log, for the roles answerable for it. */
@Service
class CommunityAuditService(
    private val membershipService: CommunityMembershipService,
    private val reader: AuditEventReader,
    private val accounts: AccountRepository,
) {
    fun list(
        communityId: UUID,
        before: UUID?,
        limit: Int?,
        accountId: UUID,
    ): List<AuditEntry> {
        membershipService.requireAnyRole(accountId, communityId, AUDIT_READERS)
        val events = reader.forCommunity(communityId, before, (limit ?: DEFAULT_PAGE).coerceIn(1, MAX_PAGE))
        val names = accounts.findAllById(events.mapNotNull { it.actorAccountId }.distinct()).associateBy({ it.id }, { it.displayName })
        return events.map { AuditEntry(it, names[it.actorAccountId]) }
    }
}
