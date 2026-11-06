package net.interlecto.proho.properties

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.communities.CommunityMembershipService
import net.interlecto.proho.communities.Membership
import net.interlecto.proho.communities.Role
import net.interlecto.proho.shared.RecordArchivedException
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

class BuildingNotInCommunityException(val buildingId: UUID) : RuntimeException("Building does not belong to this community")

class BuildingNotFoundException(val buildingId: UUID) : RuntimeException("Building not found in this community")

class UnitNotFoundException(val unitId: UUID) : RuntimeException("Unit not found in this community")

class BuildingInUseException(val buildingId: UUID) : RuntimeException("Building still has active units")

class UnitInUseException(val unitId: UUID) : RuntimeException("Unit is still in use")

data class UnitDetails(
    val number: String,
    val kind: String,
    val buildingId: UUID?,
    val floor: String?,
    val areaM2: BigDecimal?,
    val registryId: String?,
)

@Service
class PropertyService(
    private val buildings: BuildingRepository,
    private val units: PropertyUnitRepository,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val membershipService: CommunityMembershipService,
    private val auditEventRecorder: AuditEventRecorder,
    private val unitArchiveGuards: List<UnitArchiveGuard>,
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
        audit("building", building.id, "created", accountId, membership, name)
        return building
    }

    /** Archived records are for administrators restoring them, so only they can list them. */
    fun listBuildings(
        communityId: UUID,
        includeArchived: Boolean,
        accountId: UUID,
    ): List<Building> {
        if (includeArchived) {
            membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
            return buildings.findByCommunityId(communityId)
        }
        membershipService.requireActiveMembership(accountId, communityId)
        return buildings.findByCommunityIdAndArchivedAtIsNull(communityId)
    }

    fun getBuilding(
        communityId: UUID,
        buildingId: UUID,
        accountId: UUID,
    ): Building {
        membershipService.requireActiveMembership(accountId, communityId)
        return requireBuilding(communityId, buildingId)
    }

    @Transactional
    fun updateBuilding(
        communityId: UUID,
        buildingId: UUID,
        name: String,
        accountId: UUID,
    ): Building {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val building = requireBuilding(communityId, buildingId)
        if (building.archivedAt != null) {
            throw RecordArchivedException()
        }
        val updated = jdbcAggregateOperations.update(building.copy(name = name))
        audit("building", buildingId, "updated", accountId, membership, name)
        return updated
    }

    @Transactional
    fun archiveBuilding(
        communityId: UUID,
        buildingId: UUID,
        accountId: UUID,
    ): Building {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val building = requireBuilding(communityId, buildingId)
        if (building.archivedAt != null) {
            return building
        }
        if (units.existsByBuildingIdAndArchivedAtIsNull(buildingId)) {
            throw BuildingInUseException(buildingId)
        }
        val updated = jdbcAggregateOperations.update(building.copy(archivedAt = Instant.now()))
        audit("building", buildingId, "archived", accountId, membership, building.name)
        return updated
    }

    @Transactional
    fun restoreBuilding(
        communityId: UUID,
        buildingId: UUID,
        accountId: UUID,
    ): Building {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val building = requireBuilding(communityId, buildingId)
        if (building.archivedAt == null) {
            return building
        }
        val updated = jdbcAggregateOperations.update(building.copy(archivedAt = null))
        audit("building", buildingId, "restored", accountId, membership, building.name)
        return updated
    }

    @Transactional
    fun createUnit(
        communityId: UUID,
        details: UnitDetails,
        accountId: UUID,
    ): PropertyUnit {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        requireValid(communityId, details)
        if (units.existsByCommunityIdAndNumber(communityId, details.number)) {
            throw UnitNumberAlreadyRegisteredException(details.number)
        }
        val unit =
            jdbcAggregateOperations.insert(
                PropertyUnit(
                    id = Uuid7.generate(),
                    communityId = communityId,
                    buildingId = details.buildingId,
                    number = details.number,
                    kind = details.kind,
                    floor = details.floor,
                    areaM2 = details.areaM2,
                    registryId = details.registryId,
                    createdAt = Instant.now(),
                ),
            )
        audit("unit", unit.id, "created", accountId, membership, details.number)
        return unit
    }

    fun listUnits(
        communityId: UUID,
        includeArchived: Boolean,
        accountId: UUID,
    ): List<PropertyUnit> {
        if (includeArchived) {
            membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
            return units.findByCommunityId(communityId)
        }
        membershipService.requireActiveMembership(accountId, communityId)
        return units.findByCommunityIdAndArchivedAtIsNull(communityId)
    }

    fun getUnit(
        communityId: UUID,
        unitId: UUID,
        accountId: UUID,
    ): PropertyUnit {
        membershipService.requireActiveMembership(accountId, communityId)
        return requireUnit(communityId, unitId)
    }

    @Transactional
    fun updateUnit(
        communityId: UUID,
        unitId: UUID,
        details: UnitDetails,
        accountId: UUID,
    ): PropertyUnit {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val unit = requireUnit(communityId, unitId)
        if (unit.archivedAt != null) {
            throw RecordArchivedException()
        }
        requireValid(communityId, details)
        if (details.number != unit.number && units.existsByCommunityIdAndNumber(communityId, details.number)) {
            throw UnitNumberAlreadyRegisteredException(details.number)
        }
        val updated =
            jdbcAggregateOperations.update(
                unit.copy(
                    number = details.number,
                    kind = details.kind,
                    buildingId = details.buildingId,
                    floor = details.floor,
                    areaM2 = details.areaM2,
                    registryId = details.registryId,
                ),
            )
        audit("unit", unitId, "updated", accountId, membership, details.number)
        return updated
    }

    @Transactional
    fun archiveUnit(
        communityId: UUID,
        unitId: UUID,
        accountId: UUID,
    ): PropertyUnit {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val unit = requireUnit(communityId, unitId)
        if (unit.archivedAt != null) {
            return unit
        }
        if (unitArchiveGuards.any { it.isInUse(communityId, unitId) }) {
            throw UnitInUseException(unitId)
        }
        val updated = jdbcAggregateOperations.update(unit.copy(archivedAt = Instant.now()))
        audit("unit", unitId, "archived", accountId, membership, unit.number)
        return updated
    }

    @Transactional
    fun restoreUnit(
        communityId: UUID,
        unitId: UUID,
        accountId: UUID,
    ): PropertyUnit {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val unit = requireUnit(communityId, unitId)
        if (unit.archivedAt == null) {
            return unit
        }
        requireUsableBuilding(communityId, unit.buildingId)
        val updated = jdbcAggregateOperations.update(unit.copy(archivedAt = null))
        audit("unit", unitId, "restored", accountId, membership, unit.number)
        return updated
    }

    private fun requireBuilding(
        communityId: UUID,
        buildingId: UUID,
    ): Building =
        buildings.findById(buildingId).orElse(null)?.takeIf { it.communityId == communityId }
            ?: throw BuildingNotFoundException(buildingId)

    private fun requireUnit(
        communityId: UUID,
        unitId: UUID,
    ): PropertyUnit =
        units.findById(unitId).orElse(null)?.takeIf { it.communityId == communityId }
            ?: throw UnitNotFoundException(unitId)

    private fun requireValid(
        communityId: UUID,
        details: UnitDetails,
    ) {
        if (details.kind !in SUPPORTED_UNIT_KINDS) {
            throw UnsupportedUnitKindException(details.kind)
        }
        requireUsableBuilding(communityId, details.buildingId)
    }

    private fun requireUsableBuilding(
        communityId: UUID,
        buildingId: UUID?,
    ) {
        if (buildingId == null) {
            return
        }
        val building =
            buildings.findById(buildingId).orElse(null)?.takeIf { it.communityId == communityId }
                ?: throw BuildingNotInCommunityException(buildingId)
        if (building.archivedAt != null) {
            throw RecordArchivedException()
        }
    }

    private fun audit(
        entityType: String,
        entityId: UUID,
        action: String,
        accountId: UUID,
        membership: Membership,
        summary: String,
    ) {
        auditEventRecorder.record(
            entityType = entityType,
            entityId = entityId,
            action = action,
            actorAccountId = accountId,
            actorMembershipId = membership.id,
            communityId = membership.communityId,
            summary = summary,
        )
    }
}
