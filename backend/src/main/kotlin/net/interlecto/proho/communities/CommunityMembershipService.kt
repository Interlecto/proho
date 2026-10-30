package net.interlecto.proho.communities

import org.springframework.stereotype.Service
import java.util.UUID

class MembershipNotActiveException(val communityId: UUID) : RuntimeException("No active membership")

class RoleNotHeldException(val communityId: UUID, val role: Role) : RuntimeException("Role not held")

/**
 * The authorization check every `properties`/`residents` controller calls
 * before touching a community's data. Checked fresh per request, not
 * cached on the bearer-token principal, so a suspension or role change
 * takes effect immediately -- same principle as a revoked session.
 */
@Service
class CommunityMembershipService(
    private val memberships: MembershipRepository,
    private val roleAssignments: RoleAssignmentRepository,
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
}
