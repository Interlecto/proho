package net.interlecto.proho.identity

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Delivers a password-reset token to its owner. A raw token lets whoever holds
 * it take over the account, so outside local development it must never reach
 * the logs.
 */
interface PasswordResetNotifier {
    fun passwordResetRequested(
        accountId: UUID,
        rawToken: String,
    )
}

/**
 * TODO(notifications): replace with email once a notifications module exists.
 * Until then a reset requested outside the dev profile can't be completed. See
 * AccountRegistrationService's equivalent deferred-email comment.
 */
@Component
@Profile("!dev")
class UndeliveredPasswordResetNotifier : PasswordResetNotifier {
    private val log = LoggerFactory.getLogger(UndeliveredPasswordResetNotifier::class.java)

    override fun passwordResetRequested(
        accountId: UUID,
        rawToken: String,
    ) {
        log.warn("Password reset requested for account {}, but no delivery channel exists yet", accountId)
    }
}

/** Local development only: there's no email yet, so the log is how you get the token. */
@Component
@Profile("dev")
class LoggingPasswordResetNotifier : PasswordResetNotifier {
    private val log = LoggerFactory.getLogger(LoggingPasswordResetNotifier::class.java)

    override fun passwordResetRequested(
        accountId: UUID,
        rawToken: String,
    ) {
        log.info("[dev] password reset token for account {}: {}", accountId, rawToken)
    }
}
