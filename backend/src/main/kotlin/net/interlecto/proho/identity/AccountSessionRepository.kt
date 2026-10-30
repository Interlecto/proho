package net.interlecto.proho.identity

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AccountSessionRepository : CrudRepository<AccountSession, UUID> {
    fun findByTokenHash(tokenHash: String): AccountSession?

    fun findByAccountIdAndRevokedAtIsNull(accountId: UUID): List<AccountSession>
}
