package net.interlecto.proho.shared

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

/**
 * Binds the current thread to a community. Connections checked out later pick
 * it up from TenantContext, but a transaction already holds its connection, so
 * that one is updated directly. Its setting is local to the transaction.
 */
@Component
class TenantBinder(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun bindCommunity(communityId: UUID) {
        TenantContext.setCommunity(communityId)
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            jdbcTemplate.queryForObject(
                "select set_config('app.community_id', ?, true)",
                String::class.java,
                communityId.toString(),
            )
        }
    }
}
