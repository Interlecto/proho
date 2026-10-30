package net.interlecto.proho.identity

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

private const val BEARER_PREFIX = "Bearer "

class BearerTokenAuthenticationFilter(
    private val sessionAuthenticator: SessionAuthenticator,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            val rawToken = header.removePrefix(BEARER_PREFIX).trim()
            sessionAuthenticator.authenticate(rawToken)?.let { account ->
                SecurityContextHolder.getContext().authentication = BearerSessionAuthentication(account)
            }
        }
        filterChain.doFilter(request, response)
    }
}
