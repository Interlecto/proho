package net.interlecto.proho.shared

import java.util.UUID

/**
 * Who the current thread is acting for. TenantAwareDataSource copies this onto
 * every connection it hands out, where the row-level security policies in
 * V8__tenant_isolation.sql read it.
 *
 * Don't set the community directly from a request: TenantBinder does that once
 * membership has been verified, which is what makes a forgotten authorization
 * check return nothing rather than another community's data.
 */
object TenantContext {
    private val account = ThreadLocal<UUID?>()
    private val community = ThreadLocal<UUID?>()

    val accountId: UUID? get() = account.get()
    val communityId: UUID? get() = community.get()

    fun setAccount(id: UUID) = account.set(id)

    fun setCommunity(id: UUID) = community.set(id)

    fun clear() {
        account.remove()
        community.remove()
    }
}
