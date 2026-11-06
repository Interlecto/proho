package net.interlecto.proho.communities

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.identity.AccountRepository
import net.interlecto.proho.shared.TenantBinder
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class MembershipNotActiveException(val communityId: UUID) : RuntimeException("No active membership")

class RoleNotHeldException(val communityId: UUID, val role: Role) : RuntimeException("Role not held")

class MembershipNotFoundException(val membershipId: UUID) : RuntimeException("Membership not found in this community")

class PersonAlreadyLinkedException(val personId: UUID) : RuntimeException("Person is already linked to another membership")

data class Member(
    val membership: Membership,
    val displayName: String,
    val email: String,
    val roles: List<Role>,
)

/**
 * The authorization check every `properties`/`residents` controller calls
 * before touching a community's data. Checked fresh per request, not
 * cached on the bearer-token principal, so a suspension or role change
 * takes effect immediately -- same principle as a revoked session.
 *
 * Passing the check is also what binds the request to the community for
 * row-level security: code that skips it sees none of that community's rows.
 */
@Service
class CommunityMembershipService(
    private val memberships: MembershipRepository,
    private val roleAssignments: RoleAssignmentRepository,
    private val communities: CommunityRepository,
    private val tenantBinder: TenantBinder,
    private val accounts: AccountRepository,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val auditEventRecorder: AuditEventRecorder,
) {
    fun requireActiveMembership(
        accountId: UUID,
        communityId: UUID,
    ): Membership {
        val membership =
            memberships.findByAccountIdAndCommunityIdAndStatusNot(accountId, communityId, MembershipStatus.ENDED)
        if (membership == null || membership.status != MembershipStatus.ACTIVE) {
            throw MembershipNotActiveException(communityId)
        }
        tenantBinder.bindCommunity(communityId)
        return membership
    }

    fun requireRole(
        accountId: UUID,
        communityId: UUID,
        role: Role,
    ): Membership {
        val membership = requireActiveMembership(accountId, communityId)
        roleAssignments.findByMembershipIdAndRoleAndValidToIsNull(membership.id, role)
            ?: throw RoleNotHeldException(communityId, role)
        return membership
    }

    fun requireAnyRole(
        accountId: UUID,
        communityId: UUID,
        roles: Set<Role>,
    ): Membership {
        val membership = requireActiveMembership(accountId, communityId)
        if (rolesOf(membership).none { it in roles }) {
            throw RoleNotHeldException(communityId, roles.first())
        }
        return membership
    }

    /** The caller's assigned roles in force, for clients to show what applies. */
    fun rolesOf(membership: Membership): List<Role> =
        roleAssignments.findByMembershipIdAndValidToIsNull(membership.id).map { it.role }

    fun listMembers(
        communityId: UUID,
        accountId: UUID,
    ): List<Member> {
        requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val current = memberships.findByCommunityIdAndStatusNot(communityId, MembershipStatus.ENDED)
        val names = accounts.findAllById(current.map { it.accountId }).associateBy { it.id }
        return current.map {
            val account = names.getValue(it.accountId)
            Member(it, account.displayName, account.email, rolesOf(it))
        }
    }

    fun memberOf(membership: Membership): Member {
        val account = accounts.findById(membership.accountId).orElseThrow()
        return Member(membership, account.displayName, account.email, rolesOf(membership))
    }

    /** Whether a current member's login is this email. The community must already be bound. */
    fun isMemberEmail(
        communityId: UUID,
        email: String,
    ): Boolean {
        val current = memberships.findByCommunityIdAndStatusNot(communityId, MembershipStatus.ENDED)
        return accounts.findAllById(current.map { it.accountId }).any { it.email.equals(email, ignoreCase = true) }
    }

    fun isPersonLinked(personId: UUID): Boolean = memberships.existsByPersonIdAndStatusNot(personId, MembershipStatus.ENDED)

    /**
     * Gives a membership's account the person's whole record, or takes it away with
     * null. The caller has already checked that the person is in this community, and
     * the composite foreign key would refuse otherwise.
     */
    @Transactional
    fun linkPerson(
        communityId: UUID,
        membershipId: UUID,
        personId: UUID?,
        actor: Membership,
    ): Membership {
        val membership =
            memberships.findById(membershipId).orElse(null)
                ?.takeIf { it.communityId == communityId && it.status != MembershipStatus.ENDED }
                ?: throw MembershipNotFoundException(membershipId)
        if (membership.personId == personId) {
            return membership
        }
        if (personId != null && isPersonLinked(personId)) {
            throw PersonAlreadyLinkedException(personId)
        }
        val updated = jdbcAggregateOperations.update(membership.copy(personId = personId))
        auditEventRecorder.record(
            entityType = "membership",
            entityId = membershipId,
            action = if (personId == null) "person_unlinked" else "person_linked",
            actorAccountId = actor.accountId,
            actorMembershipId = actor.id,
            communityId = communityId,
        )
        return updated
    }

    /** Today in the community's time zone (domain-model.md §8). The community must already be bound. */
    fun today(communityId: UUID): LocalDate {
        val community = communities.findById(communityId).orElseThrow()
        return LocalDate.now(ZoneId.of(community.timezone))
    }
}
