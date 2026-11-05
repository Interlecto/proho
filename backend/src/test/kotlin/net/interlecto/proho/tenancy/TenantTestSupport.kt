package net.interlecto.proho.tenancy

import net.interlecto.proho.shared.TenantContext
import java.util.UUID

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
