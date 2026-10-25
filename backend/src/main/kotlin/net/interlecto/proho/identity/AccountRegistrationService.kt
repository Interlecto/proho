package net.interlecto.proho.identity

import net.interlecto.proho.shared.Uuid7
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

data class RegisterAccountCommand(
    val email: String,
    val password: String,
    val displayName: String,
    val locale: String,
)

class EmailAlreadyRegisteredException(val email: String) : RuntimeException("Email already registered")

@Service
class AccountRegistrationService(
    private val accounts: AccountRepository,
    // Account ids are pre-assigned (Uuid7), so CrudRepository.save() can't tell
    // INSERT from UPDATE the way it would for a database-generated id; it
    // would issue a no-op UPDATE instead. insert() always inserts.
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val passwordEncoder: PasswordEncoder,
) {
    // A 409 here confirms the email is registered (a user-enumeration trade-off).
    // Hardening this to a generic response plus a "someone tried to sign up
    // with your address" email is deferred until the notifications module exists.
    fun register(command: RegisterAccountCommand): Account {
        if (accounts.existsByEmail(command.email)) {
            throw EmailAlreadyRegisteredException(command.email)
        }
        val account =
            Account(
                id = Uuid7.generate(),
                email = command.email,
                displayName = command.displayName,
                locale = command.locale,
                status = AccountStatus.PENDING_VERIFICATION,
                passwordHash = passwordEncoder.encode(command.password)!!,
                createdAt = Instant.now(),
            )
        return jdbcAggregateOperations.insert(account)
    }
}
