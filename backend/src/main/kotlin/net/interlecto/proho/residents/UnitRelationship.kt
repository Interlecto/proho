package net.interlecto.proho.residents

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Table("unit_relationship")
data class UnitRelationship(
    @Id val id: UUID,
    val unitId: UUID,
    val personId: UUID,
    val role: String,
    val share: BigDecimal?,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val createdAt: Instant,
)
