package net.interlecto.proho

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTest {
    private val modules = ApplicationModules.of(ProHoApplication::class.java)

    @Test
    fun `modules respect their declared dependencies`() {
        modules.verify()
    }
}
