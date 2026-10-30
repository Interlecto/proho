package net.interlecto.proho.identity

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface PasswordResetTokenRepository : CrudRepository<PasswordResetToken, UUID> {
    fun findByTokenHash(tokenHash: String): PasswordResetToken?
}
