package net.interlecto.proho.residents

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface PersonRepository : CrudRepository<Person, UUID> {
    fun findByCommunityId(communityId: UUID): List<Person>

    fun findByCommunityIdAndArchivedAtIsNull(communityId: UUID): List<Person>

    fun existsByCommunityIdAndIdTypeAndIdNumber(
        communityId: UUID,
        idType: String,
        idNumber: String,
    ): Boolean
}
