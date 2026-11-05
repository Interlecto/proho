package net.interlecto.proho.identity

import java.util.UUID

/**
 * The request-scoped principal populated by BearerTokenAuthenticationFilter.
 * Carries everything a controller needs so GET /me doesn't need a second
 * query — the filter already loaded the account to check its status.
 */
data class AuthenticatedAccount(
    val accountId: UUID,
    val sessionId: UUID,
    val email: String,
    val displayName: String,
    val locale: String,
    val status: AccountStatus,
)
