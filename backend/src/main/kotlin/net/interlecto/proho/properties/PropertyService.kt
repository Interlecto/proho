package net.interlecto.proho.properties

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.communities.CommunityMembershipService
import net.interlecto.proho.communities.Role
import net.interlecto.proho.shared.Uuid7
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private val SUPPORTED_UNIT_KINDS =
    setOf("apartment", "house", "commercial", "office", "parking", "storage")

class UnitNumberAlreadyRegisteredException(val number: String) : RuntimeException("Unit number already registered")

class UnsupportedUnitKindException(val kind: String) : RuntimeException("Unsupported unit kind")

@Service
class PropertyService(
    private val buildings: BuildingRepository,
    private val units: PropertyUnitRepository,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val membershipService: CommunityMembershipService,
    private val auditEventRecorder: AuditEventRecorder,
) {
    @Transactional
    fun createBuilding(
        communityId: UUID,
        name: String,
        accountId: UUID,
    ): Building {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val building =
            jdbcAggregateOperations.insert(
                Building(id = Uuid7.generate(), communityId = communityId, name = name, createdAt = Instant.now()),
            )
        auditEventRecorder.record(
            entityType = "building",
            entityId = building.id,
            action = "created",
            actorAccountId = accountId,
            actorMembershipId = membership.id,
            communityId = communityId,
            summary = name,
        )
        return building
    }

    fun listBuildings(
        communityId: UUID,
        accountId: UUID,
    ): List<Building> {
        membershipService.requireActiveMembership(accountId, communityId)
        return buildings.findByCommunityId(communityId)
    }

    @Transactional
    fun createUnit(
        communityId: UUID,
        number: String,
        kind: String,
        buildingId: UUID?,
        floor: String?,
        areaM2: BigDecimal?,
        registryId: String?,
        accountId: UUID,
    ): PropertyUnit {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        if (kind !in SUPPORTED_UNIT_KINDS) {
            throw UnsupportedUnitKindException(kind)
        }
        if (units.existsByCommunityIdAndNumber(communityId, number)) {
            throw UnitNumberAlreadyRegisteredException(number)
        }
        val unit =
            jdbcAggregateOperations.insert(
                PropertyUnit(
                    id = Uuid7.generate(),
                    communityId = communityId,
                    buildingId = buildingId,
                    number = number,
                    kind = kind,
                    floor = floor,
                    areaM2 = areaM2,
                    registryId = registryId,
                    createdAt = Instant.now(),
                ),
            )
        auditEventRecorder.record(
            entityType = "unit",
            entityId = unit.id,
            action = "created",
            actorAccountId = accountId,
            actorMembershipId = membership.id,
            communityId = communityId,
            summary = number,
        )
        return unit
    }

    fun listUnits(
        communityId: UUID,
        accountId: UUID,
    ): List<PropertyUnit> {
        membershipService.requireActiveMembership(accountId, communityId)
        return units.findByCommunityId(communityId)
    }
}
