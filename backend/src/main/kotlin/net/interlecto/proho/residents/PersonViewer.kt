package net.interlecto.proho.residents

import java.time.LocalDate
import java.util.UUID

const val VISIBILITY_PRIVILEGED = "privileged"
const val VISIBILITY_COMMUNITY = "community"

val SUPPORTED_VISIBILITIES = setOf(VISIBILITY_PRIVILEGED, VISIBILITY_COMMUNITY)

/**
 * Who is reading people's records, and what that lets them see. The rules are in
 * docs/security/privacy.md. Every read of a person or a relationship by a member
 * goes through here.
 *
 * @property personId the person record linked to the caller's membership, if any
 * @property privileged holds a role in the privileged tier (Role.privileged)
 * @property seesDocuments holds a role that may read identity documents
 * @property units the units the caller has a relationship in force on today
 * @property unitmates the people with a relationship in force on those units
 */
data class PersonViewer(
    val personId: UUID?,
    val privileged: Boolean,
    val seesDocuments: Boolean,
    val today: LocalDate,
    val units: Set<UUID> = emptySet(),
    val unitmates: Set<UUID> = emptySet(),
) {
    private fun isSelf(person: Person) = person.id == personId

    /** A record this viewer can't see must look exactly like one that doesn't exist. */
    fun sees(person: Person): Boolean =
        privileged ||
            isSelf(person) ||
            (person.archivedAt == null && (person.id in unitmates || person.nameVisibility == VISIBILITY_COMMUNITY))

    /** Only the fields this viewer may read. The result is for responses, never for saving. */
    fun masked(person: Person): Person {
        val full = privileged || isSelf(person)
        // Contact details stay hidden unless the name is shared too: a phone number
        // on a record someone only sees because they share a unit isn't what was offered.
        val listed = person.nameVisibility == VISIBILITY_COMMUNITY
        val document = seesDocuments || isSelf(person)
        return person.copy(
            email = person.email.takeIf { full || (listed && person.emailVisibility == VISIBILITY_COMMUNITY) },
            phone = person.phone.takeIf { full || (listed && person.phoneVisibility == VISIBILITY_COMMUNITY) },
            idType = person.idType.takeIf { document },
            idNumber = person.idNumber.takeIf { document },
        )
    }

    /** Others see only what holds today, never a person's past addresses. */
    fun sees(
        relationship: UnitRelationship,
        person: Person,
    ): Boolean {
        if (privileged || relationship.personId == personId) {
            return true
        }
        if (!relationship.inForceOn(today) || !sees(person)) {
            return false
        }
        val sharedByPerson =
            person.nameVisibility == VISIBILITY_COMMUNITY && person.unitsVisibility == VISIBILITY_COMMUNITY
        return relationship.unitId in units || sharedByPerson
    }

    fun masked(relationship: UnitRelationship): UnitRelationship =
        if (privileged || relationship.personId == personId) relationship else relationship.copy(share = null)
}
