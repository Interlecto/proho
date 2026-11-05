package net.interlecto.proho.properties

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// Named PropertyUnit, not Unit: Kotlin's own Unit type would otherwise
// collide on every import.
@Table("unit")
data class PropertyUnit(
    @Id val id: UUID,
    val communityId: UUID,
    val buildingId: UUID?,
    val number: String,
    val kind: String,
    val floor: String?,
    val areaM2: BigDecimal?,
    val registryId: String?,
    val createdAt: Instant,
)
