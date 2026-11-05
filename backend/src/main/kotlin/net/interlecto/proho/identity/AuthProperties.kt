package net.interlecto.proho.identity

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "proho.auth")
data class AuthProperties(
    val sessionTtl: Duration = Duration.ofDays(30),
    val passwordResetTokenTtl: Duration = Duration.ofHours(1),
)
