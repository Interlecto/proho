package net.interlecto.proho.home

import net.interlecto.proho.communities.CommunityResponse
import net.interlecto.proho.communities.MembershipNotActiveException
import net.interlecto.proho.communities.toResponse
import net.interlecto.proho.identity.AuthenticatedAccount
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class MyUnitResponse(
    val unitId: UUID,
    val number: String,
    val kind: String,
    val building: String?,
    val roles: List<String>,
)

data class AdministrationSummaryResponse(
    val buildings: Int,
    val units: Int,
    val people: Int,
    val members: Int,
    val suspendedMembers: Int,
    val pendingInvitations: Int,
)

data class HomeResponse(
    val community: CommunityResponse,
    val membershipId: UUID,
    val personId: UUID?,
    val roles: List<String>,
    val units: List<MyUnitResponse>,
    val administration: AdministrationSummaryResponse?,
)

@RestController
class HomeController(
    private val homeService: HomeService,
) {
    @GetMapping("/communities/{communityId}/home")
    fun home(
        @PathVariable communityId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): HomeResponse {
        val home = homeService.home(communityId, account.accountId)
        return HomeResponse(
            community = home.community.toResponse(),
            membershipId = home.membershipId,
            personId = home.personId,
            roles = home.roles.map { it.value },
            units = home.units.map { MyUnitResponse(it.unitId, it.number, it.kind, it.building, it.roles) },
            administration =
                home.administration?.let {
                    AdministrationSummaryResponse(it.buildings, it.units, it.people, it.members, it.suspendedMembers, it.pendingInvitations)
                },
        )
    }

    @ExceptionHandler(MembershipNotActiveException::class)
    fun handleMembershipNotActive(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "membership_not_active"))
}
