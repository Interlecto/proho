package net.interlecto.proho.communities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("community")
data class Community(
    @Id val id: UUID,
    val key: String,
    val name: String,
    val legalName: String?,
    val taxId: String?,
    val kind: String,
    val plan: String?,
    val country: String,
    val currency: String,
    val timezone: String,
    val address: String?,
    val createdAt: Instant,
)
