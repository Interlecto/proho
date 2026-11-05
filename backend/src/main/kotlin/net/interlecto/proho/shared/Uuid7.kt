package net.interlecto.proho.shared

import java.security.SecureRandom
import java.util.UUID

/**
 * UUIDv7 (RFC 9562): a millisecond timestamp in the high bits followed by
 * random bits, so primary keys stay roughly time-ordered (better index
 * locality than UUIDv4) while remaining unguessable. See
 * docs/architecture/domain-model.md §8 ("Identifiers: UUIDv7 for every
 * entity").
 */
object Uuid7 {
    private val random = SecureRandom()

    fun generate(): UUID {
        val unixTsMs = System.currentTimeMillis() and 0xFFFFFFFFFFFFL
        val randA = random.nextInt(1 shl 12).toLong() and 0xFFFL
        val mostSigBits = (unixTsMs shl 16) or (0x7L shl 12) or randA

        val randBBytes = ByteArray(8)
        random.nextBytes(randBBytes)
        var randB = 0L
        for (byte in randBBytes) {
            randB = (randB shl 8) or (byte.toLong() and 0xFFL)
        }
        randB = randB and 0x3FFFFFFFFFFFFFFFL
        val leastSigBits = (0b10L shl 62) or randB

        return UUID(mostSigBits, leastSigBits)
    }
}
