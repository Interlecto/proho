package net.interlecto.proho.home

import net.interlecto.proho.communities.Community
import net.interlecto.proho.communities.CommunityMembershipService
import net.interlecto.proho.communities.CommunityRepository
import net.interlecto.proho.communities.InvitationRepository
import net.interlecto.proho.communities.MembershipRepository
import net.interlecto.proho.communities.MembershipStatus
import net.interlecto.proho.communities.Role
import net.interlecto.proho.properties.BuildingRepository
import net.interlecto.proho.properties.PropertyUnitRepository
import net.interlecto.proho.residents.PersonRepository
import net.interlecto.proho.residents.UnitRelationshipRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/** One of the caller's own units today, with the roles they hold on it. */
data class MyUnit(
    val unitId: UUID,
    val number: String,
    val kind: String,
    val building: String?,
    val roles: List<String>,
)

/** Counts an administrator watches. Records archived or ended aren't counted. */
data class AdministrationSummary(
    val buildings: Int,
    val units: Int,
    val people: Int,
    val members: Int,
    val suspendedMembers: Int,
    val pendingInvitations: Int,
)

data class Home(
    val community: Community,
    val membershipId: UUID,
    val personId: UUID?,
    val roles: List<Role>,
    val units: List<MyUnit>,
    val administration: AdministrationSummary?,
)

@Service
class HomeService(
    private val membershipService: CommunityMembershipService,
    private val communities: CommunityRepository,
    private val memberships: MembershipRepository,
    private val invitations: InvitationRepository,
    private val buildings: BuildingRepository,
    private val units: PropertyUnitRepository,
    private val people: PersonRepository,
    private val unitRelationships: UnitRelationshipRepository,
) {
    fun home(
        communityId: UUID,
        accountId: UUID,
    ): Home {
        val membership = membershipService.requireActiveMembership(accountId, communityId)
        val roles = membershipService.rolesOf(membership)
        val community = communities.findById(communityId).orElseThrow()
        return Home(
            community = community,
            membershipId = membership.id,
            personId = membership.personId,
            roles = roles,
            units = membership.personId?.let { myUnits(communityId, it) } ?: emptyList(),
            administration = if (Role.ADMINISTRATOR in roles) administration(communityId) else null,
        )
    }

    /** Units where the caller's own person record has a relationship in force today. */
    private fun myUnits(
        communityId: UUID,
        personId: UUID,
    ): List<MyUnit> {
        val today = membershipService.today(communityId)
        val current = unitRelationships.findByPersonId(personId).filter { it.inForceOn(today) }
        if (current.isEmpty()) return emptyList()
        val unitsById = units.findAllById(current.map { it.unitId }.toSet()).associateBy { it.id }
        val buildingNames =
            buildings.findAllById(unitsById.values.mapNotNull { it.buildingId }.toSet()).associate { it.id to it.name }
        return current.groupBy { it.unitId }.mapNotNull { (unitId, relationships) ->
            val unit = unitsById[unitId] ?: return@mapNotNull null
            MyUnit(unit.id, unit.number, unit.kind, unit.buildingId?.let(buildingNames::get), relationships.map { it.role }.distinct())
        }.sortedWith(compareBy({ it.building ?: "" }, { it.number }))
    }

    private fun administration(communityId: UUID): AdministrationSummary {
        val current = memberships.findByCommunityIdAndStatusNot(communityId, MembershipStatus.ENDED)
        val now = Instant.now()
        return AdministrationSummary(
            buildings = buildings.findByCommunityIdAndArchivedAtIsNull(communityId).size,
            units = units.findByCommunityIdAndArchivedAtIsNull(communityId).size,
            people = people.findByCommunityIdAndArchivedAtIsNull(communityId).size,
            members = current.count { it.status == MembershipStatus.ACTIVE },
            suspendedMembers = current.count { it.status == MembershipStatus.SUSPENDED },
            pendingInvitations =
                invitations.findByCommunityIdAndAcceptedAtIsNullAndRevokedAtIsNull(communityId).count { it.expiresAt > now },
        )
    }
}
