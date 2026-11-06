package net.interlecto.proho.properties

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface BuildingRepository : CrudRepository<Building, UUID> {
    fun findByCommunityId(communityId: UUID): List<Building>

    fun findByCommunityIdAndArchivedAtIsNull(communityId: UUID): List<Building>
}
