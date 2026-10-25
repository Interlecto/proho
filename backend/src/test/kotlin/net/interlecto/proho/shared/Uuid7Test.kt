package net.interlecto.proho.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Uuid7Test {
    @Test
    fun `generates unique, version 7 identifiers`() {
        val ids = (1..1000).map { Uuid7.generate() }

        assertThat(ids.toSet()).hasSize(1000)
        assertThat(ids).allMatch { it.version() == 7 }
    }

    @Test
    fun `is monotonically increasing between calls a millisecond apart`() {
        val first = Uuid7.generate()
        Thread.sleep(2)
        val second = Uuid7.generate()

        assertThat(first.toString() < second.toString()).isTrue()
    }
}
