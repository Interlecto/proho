package net.interlecto.proho.shared

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource

/**
 * Applies TenantContext to every connection at checkout. Always overwriting,
 * including with "no context", means a pooled connection can't carry a previous
 * request's tenant into the next one.
 */
class TenantAwareDataSource(target: DataSource) : DelegatingDataSource(target) {
    override fun getConnection(): Connection = applyContext(super.getConnection())

    override fun getConnection(
        username: String,
        password: String,
    ): Connection = applyContext(super.getConnection(username, password))

    private fun applyContext(connection: Connection): Connection {
        try {
            connection.prepareStatement(
                "select set_config('app.account_id', ?, false), set_config('app.community_id', ?, false)",
            ).use { statement ->
                statement.setString(1, TenantContext.accountId?.toString().orEmpty())
                statement.setString(2, TenantContext.communityId?.toString().orEmpty())
                statement.execute()
            }
            return connection
        } catch (e: Exception) {
            connection.close()
            throw e
        }
    }
}

/**
 * Wraps the application's DataSource bean. Flyway isn't affected: it has its
 * own connection (spring.flyway.*), as the role that owns the tables.
 */
@Component
class TenantAwareDataSourcePostProcessor : BeanPostProcessor {
    override fun postProcessAfterInitialization(
        bean: Any,
        beanName: String,
    ): Any = if (bean is DataSource && bean !is TenantAwareDataSource) TenantAwareDataSource(bean) else bean
}
