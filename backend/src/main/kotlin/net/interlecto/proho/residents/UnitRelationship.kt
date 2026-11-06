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
    val communityId: UUID,
    val unitId: UUID,
    val personId: UUID,
    val role: String,
    val share: BigDecimal?,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val createdAt: Instant,
) {
    /** Both ends of the period are inclusive: a relationship ending June 30 still holds that day. */
    fun inForceOn(day: LocalDate): Boolean = validFrom <= day && (validTo == null || validTo >= day)

    fun overlaps(
        from: LocalDate,
        to: LocalDate?,
    ): Boolean = (to == null || validFrom <= to) && (validTo == null || validTo >= from)

    /** Still running after [day], or not started yet. An end date of [day] itself counts as finished. */
    fun openAfter(day: LocalDate): Boolean = validTo == null || validTo > day
}
