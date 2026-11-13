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
//
// `privileged` is the visibility tier in domain-model.md §3, and `seesDocuments`
// the smaller group that may read identity documents (docs/security/privacy.md).
enum class Role(val value: String, val privileged: Boolean, val seesDocuments: Boolean) {
    ADMINISTRATOR("administrator", privileged = true, seesDocuments = true),
    ACCOUNTANT("accountant", privileged = true, seesDocuments = true),
    TREASURER("treasurer", privileged = true, seesDocuments = false),
    AUDITOR("auditor", privileged = true, seesDocuments = true),
    COUNCIL_MEMBER("council_member", privileged = true, seesDocuments = false),
    MODERATOR("moderator", privileged = false, seesDocuments = false),
    STAFF("staff", privileged = false, seesDocuments = false),
    ;

    companion object {
        fun fromValue(value: String): Role = entries.first { it.value == value }
    }
}
