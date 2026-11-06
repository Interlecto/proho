package net.interlecto.proho.properties

import java.util.UUID

/**
 * Lets a module that depends on `properties` stop a unit being archived while it
 * still uses it, without `properties` depending back on that module. `residents`
 * implements this today; `billing` will when it lands.
 */
fun interface UnitArchiveGuard {
    fun isInUse(
        communityId: UUID,
        unitId: UUID,
    ): Boolean
}
