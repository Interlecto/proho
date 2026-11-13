package net.interlecto.proho.properties

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("building")
data class Building(
    @Id val id: UUID,
    val communityId: UUID,
    val name: String,
    val createdAt: Instant,
    val archivedAt: Instant? = null,
)
