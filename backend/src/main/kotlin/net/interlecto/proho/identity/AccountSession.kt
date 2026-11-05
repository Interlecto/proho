package net.interlecto.proho.identity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("account_session")
data class AccountSession(
    @Id val id: UUID,
    val accountId: UUID,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant?,
)
