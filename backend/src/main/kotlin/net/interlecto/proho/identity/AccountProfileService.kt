package net.interlecto.proho.identity

import net.interlecto.proho.audit.AuditEventRecorder
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** What an account holder changes about their own login. Email changes need verification, which doesn't exist yet. */
@Service
class AccountProfileService(
    private val accounts: AccountRepository,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val auditEventRecorder: AuditEventRecorder,
) {
    @Transactional
    fun update(
        accountId: UUID,
        displayName: String,
        locale: String,
    ): Account {
        val account = accounts.findById(accountId).orElseThrow()
        val updated = jdbcAggregateOperations.update(account.copy(displayName = displayName, locale = locale))
        auditEventRecorder.record(
            entityType = "account",
            entityId = accountId,
            action = "profile_updated",
            actorAccountId = accountId,
        )
        return updated
    }
}
