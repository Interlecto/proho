package net.interlecto.proho.communities

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.shared.Uuid7
import net.interlecto.proho.shared.TenantBinder
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// MVP ships only one kind; others (ph_mixed, ph_commercial, club, ...) are
// later work per domain-model.md §10.2.
private val SUPPORTED_KINDS = setOf("ph_residential")

data class CreateCommunityCommand(
    val key: String,
    val name: String,
    val legalName: String?,
    val taxId: String?,
    val kind: String,
    val plan: String?,
    val country: String,
    val currency: String,
    val timezone: String,
    val address: String?,
)

class CommunityKeyAlreadyRegisteredException(val key: String) : RuntimeException("Community key already registered")

class UnsupportedCommunityKindException(val kind: String) : RuntimeException("Unsupported community kind")

@Service
class CommunityService(
    private val tenantBinder: TenantBinder,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val auditEventRecorder: AuditEventRecorder,
) {
    @Transactional
    fun create(
        command: CreateCommunityCommand,
        creatorAccountId: UUID,
    ): Community {
        if (command.kind !in SUPPORTED_KINDS) {
            throw UnsupportedCommunityKindException(command.kind)
        }

        // Row-level security only lets a community be created while it's the
        // bound one. It also hides other communities' keys from any lookup, so
        // the unique index is what detects a key that's already taken.
        val communityId = Uuid7.generate()
        tenantBinder.bindCommunity(communityId)

        val community =
            insertCommunity(
                Community(
                    id = communityId,
                    key = command.key,
                    name = command.name,
                    legalName = command.legalName,
                    taxId = command.taxId,
                    kind = command.kind,
                    plan = command.plan,
                    country = command.country,
                    currency = command.currency,
                    timezone = command.timezone,
                    address = command.address,
                    createdAt = Instant.now(),
                ),
            )

        val now = Instant.now()
        val membership =
            jdbcAggregateOperations.insert(
                Membership(
                    id = Uuid7.generate(),
                    accountId = creatorAccountId,
                    communityId = community.id,
                    personId = null,
                    status = MembershipStatus.ACTIVE,
                    invitedBy = null,
                    invitedAt = null,
                    joinedAt = now,
                    endedAt = null,
                    createdAt = now,
                ),
            )

        jdbcAggregateOperations.insert(
            RoleAssignment(
                id = Uuid7.generate(),
                membershipId = membership.id,
                communityId = community.id,
                role = Role.ADMINISTRATOR,
                validFrom = LocalDate.now(),
                validTo = null,
                createdAt = now,
            ),
        )

        auditEventRecorder.record(
            entityType = "community",
            entityId = community.id,
            action = "created",
            actorAccountId = creatorAccountId,
            actorMembershipId = membership.id,
            communityId = community.id,
        )
        auditEventRecorder.record(
            entityType = "membership",
            entityId = membership.id,
            action = "founded",
            actorAccountId = creatorAccountId,
            actorMembershipId = membership.id,
            communityId = community.id,
            summary = "administrator",
        )

        return community
    }

    private fun insertCommunity(community: Community): Community =
        try {
            jdbcAggregateOperations.insert(community)
        } catch (e: DataAccessException) {
            // Spring Data JDBC wraps the driver's error in its own exception.
            if (generateSequence<Throwable>(e) { it.cause }.any { it is DuplicateKeyException }) {
                throw CommunityKeyAlreadyRegisteredException(community.key)
            }
            throw e
        }
}
