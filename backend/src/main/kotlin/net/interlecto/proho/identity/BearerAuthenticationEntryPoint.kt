package net.interlecto.proho.identity

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

/**
 * The one 401 shape no controller @ExceptionHandler can produce, since this
 * fires before any controller method runs. Keeps the same flat
 * {"error": "..."} body convention as AccountController's handlers.
 */
class BearerAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("""{"error":"unauthenticated"}""")
    }
}
