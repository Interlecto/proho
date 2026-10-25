package net.interlecto.proho.identity

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AccountRepository : CrudRepository<Account, UUID> {
    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): Account?
}
