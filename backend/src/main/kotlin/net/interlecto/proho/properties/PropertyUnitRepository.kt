package net.interlecto.proho.properties

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface PropertyUnitRepository : CrudRepository<PropertyUnit, UUID> {
    fun findByCommunityId(communityId: UUID): List<PropertyUnit>

    fun findByCommunityIdAndArchivedAtIsNull(communityId: UUID): List<PropertyUnit>

    fun existsByBuildingIdAndArchivedAtIsNull(buildingId: UUID): Boolean

    fun existsByCommunityIdAndNumber(
        communityId: UUID,
        number: String,
    ): Boolean
}
