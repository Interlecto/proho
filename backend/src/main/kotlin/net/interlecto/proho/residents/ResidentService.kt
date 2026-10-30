package net.interlecto.proho.residents

import net.interlecto.proho.audit.AuditEventRecorder
import net.interlecto.proho.communities.CommunityMembershipService
import net.interlecto.proho.communities.Role
import net.interlecto.proho.properties.PropertyUnitRepository
import net.interlecto.proho.shared.Uuid7
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val SUPPORTED_PERSON_KINDS = setOf("natural", "legal")
private val SUPPORTED_RELATIONSHIP_ROLES =
    setOf("owner", "tenant", "resident", "household_member", "representative")
private val MAX_OWNER_SHARE = BigDecimal("100")

class UnsupportedPersonKindException(val kind: String) : RuntimeException("Unsupported person kind")

class InvalidPersonNameException : RuntimeException("Missing required name field for this person kind")

class PersonIdAlreadyRegisteredException : RuntimeException("Identity document already registered")

class UnsupportedRelationshipRoleException(val role: String) : RuntimeException("Unsupported relationship role")

class UnitNotInCommunityException(val unitId: UUID) : RuntimeException("Unit does not belong to this community")

class PersonNotInCommunityException(val personId: UUID) : RuntimeException("Person does not belong to this community")

class OwnerShareExceeds100Exception : RuntimeException("Owner shares for this unit would exceed 100")

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
        kind: String,
        givenName: String?,
        surname: String?,
        legalName: String?,
        idType: String?,
        idNumber: String?,
        accountId: UUID,
    ): Person {
        val membership = membershipService.requireRole(accountId, communityId, Role.ADMINISTRATOR)
        if (kind !in SUPPORTED_PERSON_KINDS) {
            throw UnsupportedPersonKindException(kind)
        }
        if (kind == "natural" && (givenName.isNullOrBlank() || surname.isNullOrBlank())) {
            throw InvalidPersonNameException()
        }
        if (kind == "legal" && legalName.isNullOrBlank()) {
            throw InvalidPersonNameException()
        }
        if (idNumber != null && people.findByCommunityId(communityId).any { it.idType == idType && it.idNumber == idNumber }) {
            throw PersonIdAlreadyRegisteredException()
        }

        val person =
            jdbcAggregateOperations.insert(
                Person(
                    id = Uuid7.generate(),
                    communityId = communityId,
                    kind = kind,
                    givenName = givenName,
                    surname = surname,
                    legalName = legalName,
                    idType = idType,
                    idNumber = idNumber,
                    createdAt = Instant.now(),
                ),
            )
        auditEventRecorder.record(
            entityType = "person",
            entityId = person.id,
            action = "created",
            actorAccountId = accountId,
            actorMembershipId = membership.id,
            communityId = communityId,
        )
        return person
    }

    fun listPeople(
        communityId: UUID,
        accountId: UUID,
    ): List<Person> {
        membershipService.requireActiveMembership(accountId, communityId)
        return people.findByCommunityId(communityId)
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
        val unit = units.findById(unitId).orElse(null)
        if (unit == null || unit.communityId != communityId) {
            throw UnitNotInCommunityException(unitId)
        }
        val person = people.findById(personId).orElse(null)
        if (person == null || person.communityId != communityId) {
            throw PersonNotInCommunityException(personId)
        }

        val effectiveShare = if (role == "owner") share ?: BigDecimal.ZERO else null
        if (role == "owner") {
            val activeOwnerShares =
                unitRelationships.findByUnitIdAndRoleAndValidToIsNull(unitId, "owner").sumOf { it.share ?: BigDecimal.ZERO }
            if (activeOwnerShares + effectiveShare!! > MAX_OWNER_SHARE) {
                throw OwnerShareExceeds100Exception()
            }
        }

        val relationship =
            jdbcAggregateOperations.insert(
                UnitRelationship(
                    id = Uuid7.generate(),
                    unitId = unitId,
                    personId = personId,
                    role = role,
                    share = effectiveShare,
                    validFrom = validFrom ?: LocalDate.now(),
                    validTo = validTo,
                    createdAt = Instant.now(),
                ),
            )
        auditEventRecorder.record(
            entityType = "unit_relationship",
            entityId = relationship.id,
            action = "created",
            actorAccountId = accountId,
            actorMembershipId = membership.id,
            communityId = communityId,
            summary = "$role on unit $unitId",
        )
        return relationship
    }

    fun listUnitRelationships(
        communityId: UUID,
        unitId: UUID,
        accountId: UUID,
    ): List<UnitRelationship> {
        membershipService.requireActiveMembership(accountId, communityId)
        val unit = units.findById(unitId).orElse(null)
        if (unit == null || unit.communityId != communityId) {
            throw UnitNotInCommunityException(unitId)
        }
        return unitRelationships.findByUnitId(unitId)
    }
}
