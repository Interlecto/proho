package net.interlecto.proho.communities

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.identity.AuthenticatedAccount
import net.interlecto.proho.identity.SecureToken
import net.interlecto.proho.shared.TenantBinder
import net.interlecto.proho.shared.Uuid7
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val INVITATION_TTL = Duration.ofDays(7)

@Table("invitation")
data class Invitation(
    @Id val id: UUID,
    val communityId: UUID,
    val email: String,
    val personId: UUID?,
    val tokenHash: String,
    val invitedBy: UUID,
    val createdAt: Instant,
    val expiresAt: Instant,
    val acceptedAt: Instant?,
    val revokedAt: Instant?,
    val membershipId: UUID?,
)

interface InvitationRepository : CrudRepository<Invitation, UUID> {
    fun findByCommunityIdAndAcceptedAtIsNullAndRevokedAtIsNull(communityId: UUID): List<Invitation>
}

/**
 * Lets `residents` say whether a person can be linked, without `communities`
 * depending back on it.
 */
fun interface PersonLinkGuard {
    /** The person exists in this community and isn't archived. */
    fun canLink(
        communityId: UUID,
        personId: UUID,
    ): Boolean
}

class InvitationInvalidException : RuntimeException("Invitation is unknown, expired, used or revoked")

class InvitationEmailMismatchException : RuntimeException("Invitation is for another email")

class InvitationPendingException : RuntimeException("This email already has an open invitation")

class InvitationNotFoundException(val invitationId: UUID) : RuntimeException("Invitation not found in this community")

class AlreadyMemberException : RuntimeException("Already a member of this community")

class InvitedPersonNotInCommunityException(val personId: UUID) : RuntimeException("Person can't be linked in this community")

/** A new invitation, and the only time its token exists outside the invitee's hands. */
data class IssuedInvitation(val invitation: Invitation, val token: String)

data class InvitationPreview(
    val communityName: String,
    val email: String,
    val matchesAccount: Boolean,
)

private data class FoundInvitation(
    val id: UUID,
    val communityId: UUID,
    val communityName: String,
    val email: String,
    val personId: UUID?,
)

/**
 * The token is the secret and the account's email must match as well, so a
 * forwarded link is useless to anyone else. Account emails aren't verified yet,
 * so neither proves control of the mailbox: see docs/security/privacy.md.
 */
@Service
class InvitationService(
    private val invitations: InvitationRepository,
    private val memberships: MembershipRepository,
    private val membershipService: CommunityMembershipService,
    private val personLinkGuard: PersonLinkGuard,
    private val tenantBinder: TenantBinder,
    private val jdbcTemplate: JdbcTemplate,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val auditEventRecorder: AuditEventRecorder,
) {
    @Transactional
    fun invite(
        communityId: UUID,
        email: String,
        personId: UUID?,
        accountId: UUID,
    ): IssuedInvitation {
        val actor = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val normalized = email.trim().lowercase()
        val now = Instant.now()

        if (membershipService.isMemberEmail(communityId, normalized)) {
            throw AlreadyMemberException()
        }
        val open = invitations.findByCommunityIdAndAcceptedAtIsNullAndRevokedAtIsNull(communityId)
        if (personId != null) {
            if (!personLinkGuard.canLink(communityId, personId)) {
                throw InvitedPersonNotInCommunityException(personId)
            }
            val promisedElsewhere = open.any { it.personId == personId && it.email != normalized && it.expiresAt > now }
            if (membershipService.isPersonLinked(personId) || promisedElsewhere) {
                throw PersonAlreadyLinkedException(personId)
            }
        }
        open.firstOrNull { it.email == normalized }?.let {
            if (it.expiresAt > now) {
                throw InvitationPendingException()
            }
            jdbcAggregateOperations.update(it.copy(revokedAt = now))
        }

        val token = SecureToken.generate()
        val invitation =
            jdbcAggregateOperations.insert(
                Invitation(
                    id = Uuid7.generate(),
                    communityId = communityId,
                    email = normalized,
                    personId = personId,
                    tokenHash = SecureToken.hash(token),
                    invitedBy = actor.id,
                    createdAt = now,
                    expiresAt = now.plus(INVITATION_TTL),
                    acceptedAt = null,
                    revokedAt = null,
                    membershipId = null,
                ),
            )
        // TODO(notifications): email the link once a notifications module exists.
        // Until then the administrator shares what this returns.
        audit(invitation.id, "created", actor)
        return IssuedInvitation(invitation, token)
    }

    fun listOpen(
        communityId: UUID,
        accountId: UUID,
    ): List<Invitation> {
        membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        return invitations.findByCommunityIdAndAcceptedAtIsNullAndRevokedAtIsNull(communityId)
    }

    @Transactional
    fun revoke(
        communityId: UUID,
        invitationId: UUID,
        accountId: UUID,
    ) {
        val actor = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val invitation =
            invitations.findById(invitationId).orElse(null)?.takeIf { it.communityId == communityId }
                ?: throw InvitationNotFoundException(invitationId)
        if (invitation.acceptedAt != null) {
            throw InvitationInvalidException()
        }
        if (invitation.revokedAt == null) {
            jdbcAggregateOperations.update(invitation.copy(revokedAt = Instant.now()))
            audit(invitationId, "revoked", actor)
        }
    }

    fun preview(
        token: String,
        account: AuthenticatedAccount,
    ): InvitationPreview {
        val found = find(token)
        return InvitationPreview(found.communityName, found.email, found.email.equals(account.email, ignoreCase = true))
    }

    /** A person who was linked or archived after the invitation joins without the link. */
    @Transactional
    fun accept(
        token: String,
        account: AuthenticatedAccount,
    ): Membership {
        val found = find(token)
        if (!found.email.equals(account.email, ignoreCase = true)) {
            throw InvitationEmailMismatchException()
        }
        // The token and email are this path's passed check, as a membership is elsewhere.
        tenantBinder.bindCommunity(found.communityId)

        // Includes a suspended membership: an invitation mustn't lift a suspension.
        val existing =
            memberships.findByAccountIdAndCommunityIdAndStatusNot(account.accountId, found.communityId, MembershipStatus.ENDED)
        if (existing != null) {
            throw AlreadyMemberException()
        }
        val invitation = invitations.findById(found.id).orElseThrow { InvitationInvalidException() }
        val personId =
            found.personId?.takeIf {
                personLinkGuard.canLink(found.communityId, it) && !membershipService.isPersonLinked(it)
            }
        val now = Instant.now()
        val membership =
            jdbcAggregateOperations.insert(
                Membership(
                    id = Uuid7.generate(),
                    accountId = account.accountId,
                    communityId = found.communityId,
                    personId = personId,
                    status = MembershipStatus.ACTIVE,
                    invitedBy = memberships.findById(invitation.invitedBy).orElse(null)?.accountId,
                    invitedAt = invitation.createdAt,
                    joinedAt = now,
                    endedAt = null,
                    createdAt = now,
                ),
            )
        jdbcAggregateOperations.update(invitation.copy(acceptedAt = now, membershipId = membership.id))
        audit(invitation.id, "accepted", membership)
        return membership
    }

    private fun find(token: String): FoundInvitation =
        jdbcTemplate.query(
            "select id, community_id, community_name, email, person_id from app_find_invitation(?)",
            { row, _ ->
                FoundInvitation(
                    id = row.getObject("id", UUID::class.java),
                    communityId = row.getObject("community_id", UUID::class.java),
                    communityName = row.getString("community_name"),
                    email = row.getString("email"),
                    personId = row.getObject("person_id", UUID::class.java),
                )
            },
            SecureToken.hash(token),
        ).firstOrNull() ?: throw InvitationInvalidException()

    private fun audit(
        invitationId: UUID,
        action: String,
        actor: Membership,
    ) {
        // No summary: the invited email is personal data.
        auditEventRecorder.record(
            entityType = "invitation",
            entityId = invitationId,
            action = action,
            actorAccountId = actor.accountId,
            actorMembershipId = actor.id,
            communityId = actor.communityId,
        )
    }
}
