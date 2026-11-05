package net.interlecto.proho.identity

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * High-entropy bearer tokens for sessions and password-reset links.
 * Distinct from shared/Uuid7.kt, which is the id convention for every
 * entity's primary key but carries only ~74 bits of randomness and encodes
 * a timestamp — unsuitable for a secret presented back as a credential.
 *
 * Only the SHA-256 hash of a token is ever persisted; the raw token exists
 * only in the response (login) or notification (password reset) that
 * first issues it.
 */
object SecureToken {
    private const val TOKEN_BYTES = 32
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
