package net.interlecto.proho.communities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Table("role_assignment")
data class RoleAssignment(
    @Id val id: UUID,
    val membershipId: UUID,
    val communityId: UUID,
    val role: Role,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val createdAt: Instant,
)

// Assigned roles only (domain-model.md §3 "Roles"). Derived roles
// (owner/tenant/resident, computed from UnitRelationship) are never stored
// here.
enum class Role(val value: String) {
    ADMINISTRATOR("administrator"),
    ACCOUNTANT("accountant"),
    TREASURER("treasurer"),
    AUDITOR("auditor"),
    COUNCIL_MEMBER("council_member"),
    MODERATOR("moderator"),
    STAFF("staff"),
    ;

    companion object {
        fun fromValue(value: String): Role = entries.first { it.value == value }
    }
}
