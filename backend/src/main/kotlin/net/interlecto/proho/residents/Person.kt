package net.interlecto.proho.residents

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("person")
data class Person(
    @Id val id: UUID,
    val communityId: UUID,
    val kind: String,
    val givenName: String?,
    val surname: String?,
    val legalName: String?,
    val idType: String?,
    val idNumber: String?,
    val createdAt: Instant,
    val email: String? = null,
    val phone: String? = null,
    val nameVisibility: String = VISIBILITY_PRIVILEGED,
    val emailVisibility: String = VISIBILITY_PRIVILEGED,
    val phoneVisibility: String = VISIBILITY_PRIVILEGED,
    val unitsVisibility: String = VISIBILITY_PRIVILEGED,
    val archivedAt: Instant? = null,
)
