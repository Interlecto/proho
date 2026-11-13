package net.interlecto.proho.residents

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.communities.CommunityMembershipService
import net.interlecto.proho.communities.Membership
import net.interlecto.proho.communities.PersonLinkGuard
import net.interlecto.proho.communities.Role
import net.interlecto.proho.properties.PropertyUnit
import net.interlecto.proho.properties.PropertyUnitRepository
import net.interlecto.proho.properties.UnitArchiveGuard
import net.interlecto.proho.properties.UnitNotFoundException
import net.interlecto.proho.shared.RecordArchivedException
import net.interlecto.proho.shared.Uuid7
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val SUPPORTED_PERSON_KINDS = setOf("natural", "legal")
private val SUPPORTED_ID_TYPES = setOf("CC", "CE", "NIT", "PA", "TI")
private val SUPPORTED_RELATIONSHIP_ROLES =
    setOf("owner", "tenant", "resident", "household_member", "representative")
private val MAX_OWNER_SHARE = BigDecimal("100")

class UnsupportedPersonKindException(val kind: String) : RuntimeException("Unsupported person kind")

class InvalidPersonNameException : RuntimeException("Missing required name field for this person kind")

class InvalidPersonIdException : RuntimeException("An identity document needs both a supported type and a number")

class PersonIdAlreadyRegisteredException : RuntimeException("Identity document already registered")

class UnsupportedRelationshipRoleException(val role: String) : RuntimeException("Unsupported relationship role")

class InvalidRelationshipPeriodException : RuntimeException("Invalid relationship period")

class PersonNotInCommunityException(val personId: UUID) : RuntimeException("Person does not belong to this community")

class PersonNotFoundException(val personId: UUID) : RuntimeException("Person not found in this community")

class RelationshipNotFoundException(val relationshipId: UUID) : RuntimeException("Relationship not found on this unit")

class RelationshipAlreadyEndedException(val relationshipId: UUID) : RuntimeException("Relationship already ended")

class PersonInUseException(val personId: UUID) : RuntimeException("Person still has open unit relationships")

class ProfileNotLinkedException : RuntimeException("This membership has no person record")

class UnsupportedVisibilityException(val visibility: String) : RuntimeException("Unsupported visibility")

class OwnerShareExceeds100Exception : RuntimeException("Owner shares for this unit would exceed 100")

data class PersonDetails(
    val kind: String,
    val givenName: String?,
    val surname: String?,
    val legalName: String?,
    val idType: String?,
    val idNumber: String?,
    val email: String? = null,
    val phone: String? = null,
)

/** What a person controls about their own record. Names and documents are the administration's. */
data class ProfileDetails(
    val email: String?,
    val phone: String?,
    val nameVisibility: String,
    val emailVisibility: String,
    val phoneVisibility: String,
    val unitsVisibility: String,
)

/** A unit with an open relationship can't be archived; end the relationship first. */
@Component
class OpenRelationshipsBlockUnitArchive(
    private val unitRelationships: UnitRelationshipRepository,
    private val membershipService: CommunityMembershipService,
) : UnitArchiveGuard {
    override fun isInUse(
        communityId: UUID,
        unitId: UUID,
    ): Boolean {
        val today = membershipService.today(communityId)
        return unitRelationships.findByUnitId(unitId).any { it.openAfter(today) }
    }
}

@Component
class ResidentPersonLinkGuard(
    private val people: PersonRepository,
) : PersonLinkGuard {
    override fun canLink(
        communityId: UUID,
        personId: UUID,
    ): Boolean = people.findById(personId).orElse(null)?.let { it.communityId == communityId && it.archivedAt == null } ?: false
}

@Service
class ResidentService(
    private val people: PersonRepository,
    private val unitRelationships: UnitRelationshipRepository,
    private val units: PropertyUnitRepository,
    private val jdbcAggregateOperations: JdbcAggregateOperations,
    private val membershipService: CommunityMembershipService,
    private val auditEventRecorder: AuditEventRecorder,
) {
    @Transactional
    fun createPerson(
        communityId: UUID,
        details: PersonDetails,
        accountId: UUID,
    ): Person {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        requireValid(details)
        requireIdAvailable(communityId, details)

        val person =
            jdbcAggregateOperations.insert(
                Person(
                    id = Uuid7.generate(),
                    communityId = communityId,
                    kind = details.kind,
                    givenName = details.givenName,
                    surname = details.surname,
                    legalName = details.legalName,
                    idType = details.idType,
                    idNumber = details.idNumber,
                    createdAt = Instant.now(),
                    email = details.email,
                    phone = details.phone,
                ),
            )
        audit("person", person.id, "created", accountId, membership)
        return person
    }

    /** Archived records are for administrators restoring them, so only they can list them. */
    fun listPeople(
        communityId: UUID,
        includeArchived: Boolean,
        accountId: UUID,
    ): List<Person> {
        if (includeArchived) {
            membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
            return people.findByCommunityId(communityId)
        }
        val viewer = viewer(communityId, membershipService.requireActiveMembership(accountId, communityId))
        return people.findByCommunityIdAndArchivedAtIsNull(communityId).filter(viewer::sees).map(viewer::masked)
    }

    fun getPerson(
        communityId: UUID,
        personId: UUID,
        accountId: UUID,
    ): Person {
        val viewer = viewer(communityId, membershipService.requireActiveMembership(accountId, communityId))
        return viewer.masked(requireVisiblePerson(communityId, personId, viewer))
    }

    @Transactional
    fun updatePerson(
        communityId: UUID,
        personId: UUID,
        details: PersonDetails,
        accountId: UUID,
    ): Person {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val person = requirePerson(communityId, personId)
        if (person.archivedAt != null) {
            throw RecordArchivedException()
        }
        requireValid(details)
        if (details.idType != person.idType || details.idNumber != person.idNumber) {
            requireIdAvailable(communityId, details)
        }
        val updated =
            jdbcAggregateOperations.update(
                person.copy(
                    kind = details.kind,
                    givenName = details.givenName,
                    surname = details.surname,
                    legalName = details.legalName,
                    idType = details.idType,
                    idNumber = details.idNumber,
                    email = details.email,
                    phone = details.phone,
                ),
            )
        // No summary: an audit entry must not carry a person's name or document.
        audit("person", personId, "updated", accountId, membership)
        return updated
    }

    @Transactional
    fun archivePerson(
        communityId: UUID,
        personId: UUID,
        accountId: UUID,
    ): Person {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val person = requirePerson(communityId, personId)
        if (person.archivedAt != null) {
            return person
        }
        val today = membershipService.today(communityId)
        if (unitRelationships.findByPersonId(personId).any { it.openAfter(today) } || membershipService.isPersonLinked(personId)) {
            throw PersonInUseException(personId)
        }
        val updated = jdbcAggregateOperations.update(person.copy(archivedAt = Instant.now()))
        audit("person", personId, "archived", accountId, membership)
        return updated
    }

    @Transactional
    fun restorePerson(
        communityId: UUID,
        personId: UUID,
        accountId: UUID,
    ): Person {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        val person = requirePerson(communityId, personId)
        if (person.archivedAt == null) {
            return person
        }
        val updated = jdbcAggregateOperations.update(person.copy(archivedAt = null))
        audit("person", personId, "restored", accountId, membership)
        return updated
    }

    fun listPersonRelationships(
        communityId: UUID,
        personId: UUID,
        accountId: UUID,
    ): List<UnitRelationship> {
        val viewer = viewer(communityId, membershipService.requireActiveMembership(accountId, communityId))
        val person = requireVisiblePerson(communityId, personId, viewer)
        return unitRelationships.findByPersonId(personId).filter { viewer.sees(it, person) }.map(viewer::masked)
    }

    /** The caller's own record. It's complete, because it's theirs. */
    fun getProfile(
        communityId: UUID,
        accountId: UUID,
    ): Person = requireLinkedPerson(communityId, membershipService.requireActiveMembership(accountId, communityId))

    @Transactional
    fun updateProfile(
        communityId: UUID,
        details: ProfileDetails,
        accountId: UUID,
    ): Person {
        val membership = membershipService.requireActiveMembership(accountId, communityId)
        val person = requireLinkedPerson(communityId, membership)
        if (person.archivedAt != null) {
            throw RecordArchivedException()
        }
        listOf(details.nameVisibility, details.emailVisibility, details.phoneVisibility, details.unitsVisibility)
            .firstOrNull { it !in SUPPORTED_VISIBILITIES }
            ?.let { throw UnsupportedVisibilityException(it) }
        val updated =
            jdbcAggregateOperations.update(
                person.copy(
                    email = details.email,
                    phone = details.phone,
                    nameVisibility = details.nameVisibility,
                    emailVisibility = details.emailVisibility,
                    phoneVisibility = details.phoneVisibility,
                    unitsVisibility = details.unitsVisibility,
                ),
            )
        audit("person", person.id, "profile_updated", accountId, membership)
        return updated
    }

    @Transactional
    fun linkMembership(
        communityId: UUID,
        membershipId: UUID,
        personId: UUID?,
        accountId: UUID,
    ) {
        val actor = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        if (personId != null) {
            val person =
                people.findById(personId).orElse(null)?.takeIf { it.communityId == communityId }
                    ?: throw PersonNotInCommunityException(personId)
            if (person.archivedAt != null) {
                throw RecordArchivedException()
            }
        }
        membershipService.linkPerson(communityId, membershipId, personId, actor)
    }

    @Transactional
    fun createUnitRelationship(
        communityId: UUID,
        unitId: UUID,
        personId: UUID,
        role: String,
        share: BigDecimal?,
        validFrom: LocalDate?,
        validTo: LocalDate?,
        accountId: UUID,
    ): UnitRelationship {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        if (role !in SUPPORTED_RELATIONSHIP_ROLES) {
            throw UnsupportedRelationshipRoleException(role)
        }
        val unit = requireUnit(communityId, unitId)
        val person =
            people.findById(personId).orElse(null)?.takeIf { it.communityId == communityId }
                ?: throw PersonNotInCommunityException(personId)
        if (unit.archivedAt != null || person.archivedAt != null) {
            throw RecordArchivedException()
        }

        val from = validFrom ?: membershipService.today(communityId)
        if (validTo != null && validTo < from) {
            throw InvalidRelationshipPeriodException()
        }
        val effectiveShare = if (role == "owner") share ?: BigDecimal.ZERO else null
        if (effectiveShare != null) {
            requireOwnerSharesFit(unitId, effectiveShare, from, validTo)
        }

        val relationship =
            jdbcAggregateOperations.insert(
                UnitRelationship(
                    id = Uuid7.generate(),
                    communityId = communityId,
                    unitId = unitId,
                    personId = personId,
                    role = role,
                    share = effectiveShare,
                    validFrom = from,
                    validTo = validTo,
                    createdAt = Instant.now(),
                ),
            )
        audit("unit_relationship", relationship.id, "created", accountId, membership, "$role on unit $unitId")
        return relationship
    }

    fun listUnitRelationships(
        communityId: UUID,
        unitId: UUID,
        accountId: UUID,
    ): List<UnitRelationship> {
        val viewer = viewer(communityId, membershipService.requireActiveMembership(accountId, communityId))
        requireUnit(communityId, unitId)
        val relationships = unitRelationships.findByUnitId(unitId)
        val owners = people.findAllById(relationships.map { it.personId }).associateBy { it.id }
        return relationships.filter { viewer.sees(it, owners.getValue(it.personId)) }.map(viewer::masked)
    }

    /**
     * Records when a relationship stops: today unless a date is given, which can be
     * past (found out late) or future (a scheduled move-out). It can only come
     * earlier than an end date already set: moving one later could break the owner
     * shares of a period that has since been filled.
     */
    @Transactional
    fun endUnitRelationship(
        communityId: UUID,
        unitId: UUID,
        relationshipId: UUID,
        validTo: LocalDate?,
        accountId: UUID,
    ): UnitRelationship {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        requireUnit(communityId, unitId)
        val relationship =
            unitRelationships.findById(relationshipId).orElse(null)?.takeIf { it.unitId == unitId }
                ?: throw RelationshipNotFoundException(relationshipId)

        val today = membershipService.today(communityId)
        if (!relationship.openAfter(today)) {
            throw RelationshipAlreadyEndedException(relationshipId)
        }
        val end = validTo ?: today
        val currentEnd = relationship.validTo
        if (end < relationship.validFrom || (currentEnd != null && end > currentEnd)) {
            throw InvalidRelationshipPeriodException()
        }

        val updated = jdbcAggregateOperations.update(relationship.copy(validTo = end))
        audit(
            "unit_relationship",
            relationshipId,
            "ended",
            accountId,
            membership,
            "${relationship.role} on unit $unitId until $end",
        )
        return updated
    }

    private fun requireValid(details: PersonDetails) {
        if (details.kind !in SUPPORTED_PERSON_KINDS) {
            throw UnsupportedPersonKindException(details.kind)
        }
        if (details.kind == "natural" && (details.givenName.isNullOrBlank() || details.surname.isNullOrBlank())) {
            throw InvalidPersonNameException()
        }
        if (details.kind == "legal" && details.legalName.isNullOrBlank()) {
            throw InvalidPersonNameException()
        }
        val idType = details.idType
        if ((idType == null) != (details.idNumber == null) || (idType != null && idType !in SUPPORTED_ID_TYPES)) {
            throw InvalidPersonIdException()
        }
    }

    private fun requireIdAvailable(
        communityId: UUID,
        details: PersonDetails,
    ) {
        val idType = details.idType ?: return
        val idNumber = details.idNumber ?: return
        if (people.existsByCommunityIdAndIdTypeAndIdNumber(communityId, idType, idNumber)) {
            throw PersonIdAlreadyRegisteredException()
        }
    }

    /** Shares only rise where a period starts, so those are the only days that need checking. */
    private fun requireOwnerSharesFit(
        unitId: UUID,
        share: BigDecimal,
        from: LocalDate,
        to: LocalDate?,
    ) {
        val overlapping = unitRelationships.findByUnitIdAndRole(unitId, "owner").filter { it.overlaps(from, to) }
        val daysToCheck = overlapping.map { maxOf(it.validFrom, from) } + from
        val peak =
            daysToCheck.maxOf { day ->
                overlapping.filter { it.inForceOn(day) }.sumOf { it.share ?: BigDecimal.ZERO }
            }
        if (peak + share > MAX_OWNER_SHARE) {
            throw OwnerShareExceeds100Exception()
        }
    }

    private fun viewer(
        communityId: UUID,
        membership: Membership,
    ): PersonViewer {
        val roles = membershipService.rolesOf(membership)
        val today = membershipService.today(communityId)
        val privileged = roles.any { it.privileged }
        val personId = membership.personId
        if (privileged || personId == null) {
            return PersonViewer(personId, privileged, roles.any { it.seesDocuments }, today)
        }
        val units = unitRelationships.findByPersonId(personId).filter { it.inForceOn(today) }.map { it.unitId }.toSet()
        val unitmates =
            units.flatMap { unitRelationships.findByUnitId(it) }.filter { it.inForceOn(today) }.map { it.personId }.toSet()
        return PersonViewer(personId, false, roles.any { it.seesDocuments }, today, units, unitmates)
    }

    private fun requireVisiblePerson(
        communityId: UUID,
        personId: UUID,
        viewer: PersonViewer,
    ): Person =
        requirePerson(communityId, personId).takeIf(viewer::sees) ?: throw PersonNotFoundException(personId)

    private fun requireLinkedPerson(
        communityId: UUID,
        membership: Membership,
    ): Person {
        val personId = membership.personId ?: throw ProfileNotLinkedException()
        return requirePerson(communityId, personId)
    }

    private fun requirePerson(
        communityId: UUID,
        personId: UUID,
    ): Person =
        people.findById(personId).orElse(null)?.takeIf { it.communityId == communityId }
            ?: throw PersonNotFoundException(personId)

    private fun requireUnit(
        communityId: UUID,
        unitId: UUID,
    ): PropertyUnit =
        units.findById(unitId).orElse(null)?.takeIf { it.communityId == communityId }
            ?: throw UnitNotFoundException(unitId)

    private fun audit(
        entityType: String,
        entityId: UUID,
        action: String,
        accountId: UUID,
        membership: Membership,
        summary: String? = null,
    ) {
        auditEventRecorder.record(
            entityType = entityType,
            entityId = entityId,
            action = action,
            actorAccountId = accountId,
            actorMembershipId = membership.id,
            communityId = membership.communityId,
            summary = summary,
        )
    }
}
