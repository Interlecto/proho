package net.interlecto.proho.identity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("account")
data class Account(
    @Id val id: UUID,
    val email: String,
    val displayName: String,
    val locale: String,
    val status: AccountStatus,
    val passwordHash: String,
    val createdAt: Instant,
)

enum class AccountStatus(val value: String) {
    PENDING_VERIFICATION("pending_verification"),
    ACTIVE("active"),
    LOCKED("locked"),
    CLOSED("closed"),
    ;

    companion object {
        fun fromValue(value: String): AccountStatus = entries.first { it.value == value }
    }
}
