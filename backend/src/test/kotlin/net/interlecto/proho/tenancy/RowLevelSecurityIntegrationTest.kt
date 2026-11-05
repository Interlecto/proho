package net.interlecto.proho.tenancy

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.shared.Uuid7
import net.interlecto.proho.shared.TenantBinder
import net.interlecto.proho.shared.TenantContext
import net.interlecto.proho.shared.TenantIsolationGuard
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.sql.SQLException
import java.util.UUID

/**
 * The policies themselves, as the application's own role, with no service-layer
 * checks in the way. The superuser sets up data and confirms it exists, so an
 * empty result always means "hidden", never "not there".
 */
@SpringBootTest
class RowLevelSecurityIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var app: JdbcTemplate

    @Autowired
    lateinit var transactions: TransactionTemplate

    @Autowired
    lateinit var binder: TenantBinder

    private val superuser = EmbeddedPostgres.superuser()

    private data class Tenant(
        val communityId: UUID,
        val accountId: UUID,
        val membershipId: UUID,
        val buildingId: UUID,
        val unitId: UUID,
        val personId: UUID,
    )

    private lateinit var a: Tenant
    private lateinit var b: Tenant

    private val tenantTables =
        listOf("building", "unit", "person", "membership", "role_assignment", "unit_relationship", "community")

    private fun tenant(): Tenant {
        val tag = UUID.randomUUID().toString()
        val t =
            Tenant(
                communityId = Uuid7.generate(),
                accountId = Uuid7.generate(),
                membershipId = Uuid7.generate(),
                buildingId = Uuid7.generate(),
                unitId = Uuid7.generate(),
                personId = Uuid7.generate(),
            )
        superuser.update(
            "insert into community (id, key, name, kind) values (?, ?, 'C', 'ph_residential')",
            t.communityId,
            "rls-$tag",
        )
        superuser.update(
            "insert into account (id, email, display_name, password_hash) values (?, ?, 'A', 'x')",
            t.accountId,
            "rls-$tag@example.com",
        )
        superuser.update(
            "insert into membership (id, account_id, community_id, status) values (?, ?, ?, 'active')",
            t.membershipId,
            t.accountId,
            t.communityId,
        )
        superuser.update(
            "insert into role_assignment (id, membership_id, community_id, role, valid_from) values (?, ?, ?, 'administrator', current_date)",
            Uuid7.generate(),
            t.membershipId,
            t.communityId,
        )
        superuser.update("insert into building (id, community_id, name) values (?, ?, 'B')", t.buildingId, t.communityId)
        superuser.update(
            "insert into unit (id, community_id, building_id, number, kind) values (?, ?, ?, '1', 'apartment')",
            t.unitId,
            t.communityId,
            t.buildingId,
        )
        superuser.update(
            "insert into person (id, community_id, kind, given_name, surname) values (?, ?, 'natural', 'P', 'Q')",
            t.personId,
            t.communityId,
        )
        superuser.update(
            "insert into unit_relationship (id, community_id, unit_id, person_id, role, valid_from) values (?, ?, ?, ?, 'resident', current_date)",
            Uuid7.generate(),
            t.communityId,
            t.unitId,
            t.personId,
        )
        return t
    }

    @BeforeEach
    fun setUp() {
        a = tenant()
        b = tenant()
    }

    @AfterEach
    fun clearContext() = TenantContext.clear()

    /**
     * SQLSTATE 42501, for both an RLS violation and a missing privilege. Not the
     * message: PostgreSQL localizes it to the server's language.
     */
    private fun assertDenied(statement: () -> Unit) {
        assertThatThrownBy(statement)
            .isInstanceOf(DataAccessException::class.java)
            .rootCause()
            .isInstanceOfSatisfying(SQLException::class.java) { assertThat(it.sqlState).isEqualTo("42501") }
    }

    private fun communityIds(
        jdbc: JdbcTemplate,
        table: String,
    ): List<UUID?> {
        val column = if (table == "community") "id" else "community_id"
        return jdbc.queryForList("select $column from $table", UUID::class.java)
    }

    @Test
    fun `with no context the application sees no tenant rows at all`() {
        tenantTables.forEach { table ->
            assertThat(communityIds(superuser, table)).describedAs(table).contains(a.communityId, b.communityId)
            assertThat(communityIds(app, table)).describedAs(table).isEmpty()
        }
    }

    @Test
    fun `a bound community sees only its own rows`() {
        asCommunity(a.communityId) {
            tenantTables.forEach { table ->
                assertThat(communityIds(app, table)).describedAs(table).isNotEmpty.containsOnly(a.communityId)
            }
        }
    }

    @Test
    fun `another community's rows can't be fetched by id`() {
        asCommunity(a.communityId) {
            val count =
                app.queryForObject("select count(*) from person where id = ?", Long::class.java, b.personId)
            assertThat(count).isZero()
        }
    }

    @Test
    fun `rows can't be written into another community`() {
        asCommunity(a.communityId) {
            assertDenied {
                app.update("insert into building (id, community_id, name) values (?, ?, 'X')", Uuid7.generate(), b.communityId)
            }

            assertDenied {
                app.update("update building set community_id = ? where id = ?", b.communityId, a.buildingId)
            }
        }
    }

    @Test
    fun `another community's rows can't be updated`() {
        asCommunity(a.communityId) {
            assertThat(app.update("update building set name = 'hacked' where id = ?", b.buildingId)).isZero()
        }
        val name = superuser.queryForObject("select name from building where id = ?", String::class.java, b.buildingId)
        assertThat(name).isEqualTo("B")
    }

    @Test
    fun `an account sees its own memberships and communities, and no one else's, with none bound`() {
        asAccount(a.accountId) {
            assertThat(communityIds(app, "membership")).containsOnly(a.communityId)
            assertThat(communityIds(app, "community")).containsOnly(a.communityId)
            assertThat(communityIds(app, "unit")).isEmpty()
            assertThat(communityIds(app, "person")).isEmpty()
        }
    }

    @Test
    fun `an account can't add itself to a community without having it bound`() {
        asAccount(a.accountId) {
            assertDenied {
                app.update(
                    "insert into membership (id, account_id, community_id, status) values (?, ?, ?, 'active')",
                    Uuid7.generate(),
                    a.accountId,
                    b.communityId,
                )
            }
        }
    }

    @Test
    fun `an ended membership no longer reveals the community`() {
        superuser.update("update membership set status = 'ended' where id = ?", a.membershipId)

        asAccount(a.accountId) {
            assertThat(communityIds(app, "community")).isEmpty()
        }
    }

    @Test
    fun `a pooled connection doesn't carry a context into the next use`() {
        asCommunity(a.communityId) {
            assertThat(communityIds(app, "unit")).containsOnly(a.communityId)
        }

        // More checkouts than the pool has connections, so the one used above
        // is certainly among them.
        repeat(30) {
            assertThat(communityIds(app, "unit")).isEmpty()
        }
    }

    @Test
    fun `binding inside a transaction applies to that transaction's connection`() {
        transactions.executeWithoutResult {
            assertThat(communityIds(app, "unit")).isEmpty()
            binder.bindCommunity(a.communityId)
            assertThat(communityIds(app, "unit")).containsOnly(a.communityId)
        }
    }

    @Test
    fun `a transaction rolled back after binding leaves nothing behind`() {
        transactions.executeWithoutResult { status ->
            binder.bindCommunity(a.communityId)
            status.setRollbackOnly()
        }
        TenantContext.clear()

        repeat(30) {
            assertThat(communityIds(app, "unit")).isEmpty()
        }
    }

    @Test
    fun `audit events are append-only for the application`() {
        val id = Uuid7.generate()
        asCommunity(a.communityId) {
            app.update(
                "insert into audit_event (id, community_id, entity_type, entity_id, action) values (?, ?, 'unit', ?, 'created')",
                id,
                a.communityId,
                a.unitId,
            )
            assertDenied { app.update("update audit_event set action = 'x' where id = ?", id) }
            assertDenied { app.update("delete from audit_event where id = ?", id) }
        }
    }

    @Test
    fun `audit events of another community are hidden and can't be forged`() {
        val id = Uuid7.generate()
        superuser.update(
            "insert into audit_event (id, community_id, entity_type, entity_id, action) values (?, ?, 'unit', ?, 'created')",
            id,
            b.communityId,
            b.unitId,
        )

        asCommunity(a.communityId) {
            assertThat(app.queryForObject("select count(*) from audit_event where id = ?", Long::class.java, id)).isZero()
            assertDenied {
                app.update(
                    "insert into audit_event (id, community_id, entity_type, entity_id, action) values (?, ?, 'unit', ?, 'created')",
                    Uuid7.generate(),
                    b.communityId,
                    b.unitId,
                )
            }
        }
    }

    @Test
    fun `the application can't delete tenant rows`() {
        asCommunity(a.communityId) {
            assertDenied { app.update("delete from person where id = ?", a.personId) }
        }
    }

    // Foreign key checks ignore RLS, so these hold for any role, superuser included.

    @Test
    fun `a unit can't point at another community's building`() {
        assertThatThrownBy {
            superuser.update(
                "insert into unit (id, community_id, building_id, number, kind) values (?, ?, ?, 'x', 'apartment')",
                Uuid7.generate(),
                a.communityId,
                b.buildingId,
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `a unit relationship can't span two communities`() {
        assertThatThrownBy {
            superuser.update(
                "insert into unit_relationship (id, community_id, unit_id, person_id, role, valid_from) values (?, ?, ?, ?, 'resident', current_date)",
                Uuid7.generate(),
                a.communityId,
                a.unitId,
                b.personId,
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `a role can't be recorded in a different community from its membership`() {
        assertThatThrownBy {
            superuser.update(
                "insert into role_assignment (id, membership_id, community_id, role, valid_from) values (?, ?, ?, 'administrator', current_date)",
                Uuid7.generate(),
                a.membershipId,
                b.communityId,
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `a membership's person must belong to the same community`() {
        assertThatThrownBy {
            superuser.update("update membership set person_id = ? where id = ?", b.personId, a.membershipId)
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun insertAuditEvent(
        communityId: UUID?,
        membershipId: UUID,
    ) = superuser.update(
        "insert into audit_event (id, community_id, actor_membership_id, entity_type, entity_id, action) " +
            "values (?, ?, ?, 'unit', ?, 'created')",
        Uuid7.generate(),
        communityId,
        membershipId,
        a.unitId,
    )

    @Test
    fun `an audit event's actor must be a member of the event's community`() {
        assertThat(insertAuditEvent(a.communityId, a.membershipId)).isEqualTo(1)
        assertThatThrownBy { insertAuditEvent(a.communityId, b.membershipId) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `an audit event with a member as actor must have a community`() {
        assertThatThrownBy { insertAuditEvent(null, a.membershipId) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    // TenantIsolationGuard

    @Test
    fun `the guard accepts the application's role`() {
        TenantIsolationGuard.verify(app)
    }

    @Test
    fun `the guard rejects a superuser`() {
        assertThatThrownBy { TenantIsolationGuard.verify(superuser) }
            .isInstanceOf(IllegalStateException::class.java).hasMessageContaining("superuser")
    }

    @Test
    fun `the guard rejects the role that owns the tables`() {
        assertThatThrownBy { TenantIsolationGuard.verify(EmbeddedPostgres.owner()) }
            .isInstanceOf(IllegalStateException::class.java).hasMessageContaining("owns")
    }

    @Test
    fun `the guard rejects a tenant table without row-level security`() {
        superuser.execute("create table rls_guard_probe (id uuid primary key, community_id uuid)")
        try {
            assertThatThrownBy { TenantIsolationGuard.verify(app) }
                .isInstanceOf(IllegalStateException::class.java).hasMessageContaining("rls_guard_probe")
        } finally {
            superuser.execute("drop table rls_guard_probe")
        }
    }
}
