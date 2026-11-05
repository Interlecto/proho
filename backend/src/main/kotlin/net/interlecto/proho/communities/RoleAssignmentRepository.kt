package net.interlecto.proho.communities

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface RoleAssignmentRepository : CrudRepository<RoleAssignment, UUID> {
    fun findByMembershipIdAndRoleAndValidToIsNull(
        membershipId: UUID,
        role: Role,
    ): RoleAssignment?
}
