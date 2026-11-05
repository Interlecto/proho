package net.interlecto.proho.communities

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MembershipRepository : CrudRepository<Membership, UUID> {
    fun findByAccountIdAndCommunityIdAndStatusNot(
        accountId: UUID,
        communityId: UUID,
        status: MembershipStatus,
    ): Membership?

    fun findByAccountIdAndStatusNot(
        accountId: UUID,
        status: MembershipStatus,
    ): List<Membership>
}
