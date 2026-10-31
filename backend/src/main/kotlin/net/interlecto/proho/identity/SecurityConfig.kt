package net.interlecto.proho.identity

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Stateless bearer-token authentication. The filter chain is fully custom:
 * no form login, no httpBasic, no server-side HTTP session, CSRF disabled
 * because there is no cookie-based credential for CSRF to exploit — every
 * request authenticates itself via the Authorization header.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val sessionAuthenticator: SessionAuthenticator,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/accounts").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/password-reset/request").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/password-reset/confirm").permitAll()
                it.requestMatchers("/actuator/**").permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(BearerAuthenticationEntryPoint()) }
            .addFilterBefore(
                BearerTokenAuthenticationFilter(sessionAuthenticator),
                UsernamePasswordAuthenticationFilter::class.java,
            )
        return http.build()
    }

    // Dev-only: lets the Vite dev server (a different origin/port) call this
    // API from a browser. Revisit before any non-local deployment.
    private fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns = listOf("http://localhost:*")
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
            }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
