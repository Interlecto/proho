package net.interlecto.proho.identity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("password_reset_token")
data class PasswordResetToken(
    @Id val id: UUID,
    val accountId: UUID,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant?,
)
