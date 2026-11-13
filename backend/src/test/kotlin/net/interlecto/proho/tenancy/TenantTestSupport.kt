package net.interlecto.proho.tenancy

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.shared.TenantContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.util.UUID
import javax.sql.DataSource

/** Runs test setup or assertions on the test's own thread, bound to a community. */
fun <T> asCommunity(
    communityId: UUID,
    block: () -> T,
): T {
    TenantContext.setCommunity(communityId)
    try {
        return block()
    } finally {
        TenantContext.clear()
    }
}

/** Runs on the test's own thread as an account, with no community bound. */
fun <T> asAccount(
    accountId: UUID,
    block: () -> T,
): T {
    TenantContext.setAccount(accountId)
    try {
        return block()
    } finally {
        TenantContext.clear()
    }
}

/** Plain SQL as the application's role, with no tenant bound. */
object TenantJdbc {
    private val app: JdbcTemplate by lazy {
        val owner = EmbeddedPostgres.owner().dataSource as DataSource
        val url = owner.connection.use { it.metaData.url }
        JdbcTemplate(DriverManagerDataSource(url, "proho_app", "proho_app"))
    }

    fun update(sql: String): Int = app.update(sql)

    fun countThroughLookup(hash: String): Int =
        app.queryForObject("select count(*) from app_find_invitation(?)", Int::class.java, hash)!!

    fun countVisibleInvitations(id: UUID): Int =
        app.queryForObject("select count(*) from invitation where id = ?", Int::class.java, id)!!
}
