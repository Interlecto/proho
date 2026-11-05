package net.interlecto.proho.identity

import org.springframework.security.authentication.AbstractAuthenticationToken

/**
 * Carries an AuthenticatedAccount as the SecurityContext principal. No
 * authorities yet — no role/permission model exists until the
 * `communities` module.
 */
class BearerSessionAuthentication(private val account: AuthenticatedAccount) :
    AbstractAuthenticationToken(emptyList()) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): AuthenticatedAccount = account
}
