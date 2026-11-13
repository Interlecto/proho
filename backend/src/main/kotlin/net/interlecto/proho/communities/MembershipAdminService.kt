package net.interlecto.proho.communities

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.shared.Uuid7
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

class UnsupportedRoleException(val role: String) : RuntimeException("Unsupported role")

class RoleAlreadyHeldException : RuntimeException("The member already holds this role")

class RoleNotAssignedException : RuntimeException("The member doesn't hold this role")

class LastAdministratorException : RuntimeException("A community must keep an active administrator")

/**
 * Administrators assign roles and suspend or remove members. Everything takes
 * effect on the member's next request, because roles and status are read fresh
 * (CommunityMembershipService). Nothing is deleted: a role gets an end date and a
 * membership becomes `ended`.
 */
@Service
class MembershipAdminService(
    private val memberships: MembershipRepository,
    private val roleAssignments: RoleAssignmentRepository,
    private val membershipService: CommunityMembershipService,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val jdbcTemplate: JdbcTemplate,
    private val auditEventRecorder: AuditEventRecorder,
) {
    @Transactional
    fun assignRole(
        communityId: UUID,
        membershipId: UUID,
        roleValue: String,
        accountId: UUID,
    ): Member {
        val actor = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val role = Role.entries.firstOrNull { it.value == roleValue } ?: throw UnsupportedRoleException(roleValue)
        val membership = requireCurrent(communityId, membershipId)
        if (roleAssignments.findByMembershipIdAndRoleAndValidToIsNull(membershipId, role) != null) {
            throw RoleAlreadyHeldException()
        }
        jdbcAggregateOperations.insert(
            RoleAssignment(
                id = Uuid7.generate(),
                membershipId = membershipId,
                communityId = communityId,
                role = role,
                validFrom = membershipService.today(communityId),
                validTo = null,
                createdAt = Instant.now(),
            ),
        )
        audit(membershipId, "role_assigned", actor, role.value)
        return membershipService.memberOf(membership)
    }

    @Transactional
    fun endRole(
        communityId: UUID,
        membershipId: UUID,
        roleValue: String,
        accountId: UUID,
    ): Member {
        val actor = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val role = Role.entries.firstOrNull { it.value == roleValue } ?: throw UnsupportedRoleException(roleValue)
        val membership = requireCurrent(communityId, membershipId)
        val assignment =
            roleAssignments.findByMembershipIdAndRoleAndValidToIsNull(membershipId, role)
                ?: throw RoleNotAssignedException()
        if (role == Role.ADMINISTRATOR) {
            requireAnotherAdministrator(communityId, membership)
        }
        jdbcAggregateOperations.update(assignment.copy(validTo = membershipService.today(communityId)))
        audit(membershipId, "role_ended", actor, role.value)
        return membershipService.memberOf(membership)
    }

    @Transactional
    fun suspend(
        communityId: UUID,
        membershipId: UUID,
        accountId: UUID,
    ): Member {
        val actor = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val membership = requireCurrent(communityId, membershipId)
        if (membership.status == MembershipStatus.SUSPENDED) {
            return membershipService.memberOf(membership)
        }
        requireAnotherAdministrator(communityId, membership)
        val updated = jdbcAggregateOperations.update(membership.copy(status = MembershipStatus.SUSPENDED))
        audit(membershipId, "suspended", actor)
        return membershipService.memberOf(updated)
    }

    @Transactional
    fun reactivate(
        communityId: UUID,
        membershipId: UUID,
        accountId: UUID,
    ): Member {
        val actor = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val membership = requireCurrent(communityId, membershipId)
        if (membership.status == MembershipStatus.ACTIVE) {
            return membershipService.memberOf(membership)
        }
        val updated = jdbcAggregateOperations.update(membership.copy(status = MembershipStatus.ACTIVE))
        audit(membershipId, "reactivated", actor)
        return membershipService.memberOf(updated)
    }

    @Transactional
    fun remove(
        communityId: UUID,
        membershipId: UUID,
        accountId: UUID,
    ) {
        val actor = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        end(communityId, requireCurrent(communityId, membershipId), actor, "removed")
    }

    /** Any active member can go. A suspended one can't, so leaving can't dodge a suspension's record. */
    @Transactional
    fun leave(
        communityId: UUID,
        accountId: UUID,
    ) {
        val membership = membershipService.requireActiveMembership(accountId, communityId)
        end(communityId, membership, membership, "left")
    }

    private fun end(
        communityId: UUID,
        membership: Membership,
        actor: Membership,
        action: String,
    ) {
        requireAnotherAdministrator(communityId, membership)
        val today = membershipService.today(communityId)
        roleAssignments.findByMembershipIdAndValidToIsNull(membership.id).forEach {
            jdbcAggregateOperations.update(it.copy(validTo = today))
        }
        jdbcAggregateOperations.update(membership.copy(status = MembershipStatus.ENDED, endedAt = Instant.now()))
        audit(membership.id, action, actor)
    }

    /**
     * Stops [membership] being the community's last active administrator. The row
     * lock makes two administrators removing each other at once run one after the
     * other, so the second sees the first's change and fails.
     */
    private fun requireAnotherAdministrator(
        communityId: UUID,
        membership: Membership,
    ) {
        jdbcTemplate.queryForObject("select id from community where id = ? for update", UUID::class.java, communityId)
        val others =
            memberships.findByCommunityIdAndStatusNot(communityId, MembershipStatus.ENDED)
                .filter { it.status == MembershipStatus.ACTIVE && it.id != membership.id }
        val isAdministrator = Role.ADMINISTRATOR in membershipService.rolesOf(membership)
        if (isAdministrator && others.none { Role.ADMINISTRATOR in membershipService.rolesOf(it) }) {
            throw LastAdministratorException()
        }
    }

    private fun requireCurrent(
        communityId: UUID,
        membershipId: UUID,
    ): Membership =
        memberships.findById(membershipId).orElse(null)
            ?.takeIf { it.communityId == communityId && it.status != MembershipStatus.ENDED }
            ?: throw MembershipNotFoundException(membershipId)

    private fun audit(
        membershipId: UUID,
        action: String,
        actor: Membership,
        summary: String? = null,
    ) {
        auditEventRecorder.record(
            entityType = "membership",
            entityId = membershipId,
            action = action,
            actorAccountId = actor.accountId,
            actorMembershipId = actor.id,
            communityId = actor.communityId,
            summary = summary,
        )
    }
}
