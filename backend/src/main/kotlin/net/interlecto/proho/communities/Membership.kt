package net.interlecto.proho.communities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("membership")
data class Membership(
    @Id val id: UUID,
    val accountId: UUID,
    val communityId: UUID,
    val personId: UUID?,
    val status: MembershipStatus,
    val invitedBy: UUID?,
    val invitedAt: Instant?,
    val joinedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
)

enum class MembershipStatus(val value: String) {
    INVITED("invited"),
    ACTIVE("active"),
    SUSPENDED("suspended"),
    ENDED("ended"),
    ;

    companion object {
        fun fromValue(value: String): MembershipStatus = entries.first { it.value == value }
    }
}
