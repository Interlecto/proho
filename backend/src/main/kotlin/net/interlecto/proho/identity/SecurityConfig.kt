package net.interlecto.proho.identity

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

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
}
