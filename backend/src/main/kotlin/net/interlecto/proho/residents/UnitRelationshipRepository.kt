package net.interlecto.proho.residents

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface UnitRelationshipRepository : CrudRepository<UnitRelationship, UUID> {
    fun findByUnitId(unitId: UUID): List<UnitRelationship>

    fun findByUnitIdAndRoleAndValidToIsNull(
        unitId: UUID,
        role: String,
    ): List<UnitRelationship>
}
