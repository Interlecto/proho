package net.interlecto.proho.communities

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import net.interlecto.proho.identity.AuthenticatedAccount
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class CreateInvitationRequest(
    @field:NotBlank @field:Email @field:Size(max = 320)
    val email: String,
    val personId: UUID? = null,
)

data class InvitationResponse(
    val id: UUID,
    val email: String,
    val personId: UUID?,
    val createdAt: Instant,
    val expiresAt: Instant,
    /** Present only in the response that creates the invitation. */
    val token: String? = null,
)

private fun Invitation.toResponse(token: String? = null) = InvitationResponse(id, email, personId, createdAt, expiresAt, token)

data class InvitationTokenRequest(
    @field:NotBlank @field:Size(max = 100)
    val token: String,
)

data class InvitationPreviewResponse(val communityName: String, val email: String, val matchesAccount: Boolean)

data class AcceptedInvitationResponse(val communityId: UUID, val membershipId: UUID, val personId: UUID?)

data class AuditEventResponse(
    val id: UUID,
    val occurredAt: Instant,
    val entityType: String,
    val entityId: UUID,
    val action: String,
    val summary: String?,
    val actorName: String?,
    val requestId: String?,
)

data class AssignRoleRequest(
    @field:NotBlank @field:Size(max = 30)
    val role: String,
)

internal fun Member.toResponse() =
    MemberResponse(
        membership.id,
        displayName,
        email,
        membership.personId,
        membership.status.value,
        roles.map { it.value },
    )

@RestController
class MembershipController(
    private val invitationService: InvitationService,
    private val adminService: MembershipAdminService,
    private val auditService: CommunityAuditService,
) {
    @GetMapping("/communities/{communityId}/audit-events")
    fun auditEvents(
        @PathVariable communityId: UUID,
        @RequestParam(required = false) before: UUID?,
        @RequestParam(required = false) limit: Int?,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<AuditEventResponse> =
        auditService.list(communityId, before, limit, account.accountId).map {
            AuditEventResponse(
                it.event.id,
                it.event.occurredAt,
                it.event.entityType,
                it.event.entityId,
                it.event.action,
                it.event.summary,
                it.actorName,
                it.event.requestId,
            )
        }

    @PostMapping("/communities/{communityId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    fun invite(
        @PathVariable communityId: UUID,
        @Valid @RequestBody request: CreateInvitationRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): InvitationResponse {
        val issued = invitationService.invite(communityId, request.email, request.personId, account.accountId)
        return issued.invitation.toResponse(issued.token)
    }

    @GetMapping("/communities/{communityId}/invitations")
    fun listInvitations(
        @PathVariable communityId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<InvitationResponse> = invitationService.listOpen(communityId, account.accountId).map { it.toResponse() }

    @PostMapping("/communities/{communityId}/invitations/{invitationId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeInvitation(
        @PathVariable communityId: UUID,
        @PathVariable invitationId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ) = invitationService.revoke(communityId, invitationId, account.accountId)

    // POST, not GET with the token in the path: URLs end up in logs.
    @PostMapping("/invitations/preview")
    fun previewInvitation(
        @Valid @RequestBody request: InvitationTokenRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): InvitationPreviewResponse {
        val preview = invitationService.preview(request.token, account)
        return InvitationPreviewResponse(preview.communityName, preview.email, preview.matchesAccount)
    }

    @PostMapping("/invitations/accept")
    fun acceptInvitation(
        @Valid @RequestBody request: InvitationTokenRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): AcceptedInvitationResponse {
        val membership = invitationService.accept(request.token, account)
        return AcceptedInvitationResponse(membership.communityId, membership.id, membership.personId)
    }

    @PostMapping("/communities/{communityId}/memberships/{membershipId}/roles")
    fun assignRole(
        @PathVariable communityId: UUID,
        @PathVariable membershipId: UUID,
        @Valid @RequestBody request: AssignRoleRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): MemberResponse = adminService.assignRole(communityId, membershipId, request.role, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/memberships/{membershipId}/roles/{role}/end")
    fun endRole(
        @PathVariable communityId: UUID,
        @PathVariable membershipId: UUID,
        @PathVariable role: String,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): MemberResponse = adminService.endRole(communityId, membershipId, role, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/memberships/{membershipId}/suspend")
    fun suspend(
        @PathVariable communityId: UUID,
        @PathVariable membershipId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): MemberResponse = adminService.suspend(communityId, membershipId, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/memberships/{membershipId}/reactivate")
    fun reactivate(
        @PathVariable communityId: UUID,
        @PathVariable membershipId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): MemberResponse = adminService.reactivate(communityId, membershipId, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/memberships/{membershipId}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @PathVariable communityId: UUID,
        @PathVariable membershipId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ) = adminService.remove(communityId, membershipId, account.accountId)

    @PostMapping("/communities/{communityId}/membership/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leave(
        @PathVariable communityId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ) = adminService.leave(communityId, account.accountId)

    @ExceptionHandler(MembershipNotActiveException::class)
    fun handleMembershipNotActive() = error(HttpStatus.FORBIDDEN, "membership_not_active")

    @ExceptionHandler(RoleNotHeldException::class)
    fun handleRoleNotHeld() = error(HttpStatus.FORBIDDEN, "role_not_held")

    @ExceptionHandler(InvitationInvalidException::class)
    fun handleInvitationInvalid() = error(HttpStatus.NOT_FOUND, "invitation_invalid")

    @ExceptionHandler(InvitationEmailMismatchException::class)
    fun handleInvitationEmailMismatch() = error(HttpStatus.FORBIDDEN, "invitation_email_mismatch")

    @ExceptionHandler(InvitationPendingException::class)
    fun handleInvitationPending() = error(HttpStatus.CONFLICT, "invitation_pending")

    @ExceptionHandler(AlreadyMemberException::class)
    fun handleAlreadyMember() = error(HttpStatus.CONFLICT, "already_member")

    @ExceptionHandler(PersonAlreadyLinkedException::class)
    fun handlePersonAlreadyLinked() = error(HttpStatus.CONFLICT, "person_already_linked")

    @ExceptionHandler(InvitedPersonNotInCommunityException::class)
    fun handleInvitedPerson() = error(HttpStatus.BAD_REQUEST, "not_found_in_community")

    @ExceptionHandler(InvitationNotFoundException::class, MembershipNotFoundException::class)
    fun handleNotFound() = error(HttpStatus.NOT_FOUND, "not_found_in_community")

    @ExceptionHandler(RoleAlreadyHeldException::class)
    fun handleRoleAlreadyHeld() = error(HttpStatus.CONFLICT, "role_already_held")

    @ExceptionHandler(RoleNotAssignedException::class)
    fun handleRoleNotAssigned() = error(HttpStatus.CONFLICT, "role_not_assigned")

    @ExceptionHandler(LastAdministratorException::class)
    fun handleLastAdministrator() = error(HttpStatus.CONFLICT, "last_administrator")

    @ExceptionHandler(UnsupportedRoleException::class, MethodArgumentNotValidException::class)
    fun handleInvalidRequest() = error(HttpStatus.BAD_REQUEST, "invalid_request")

    private fun error(
        status: HttpStatus,
        code: String,
    ): ResponseEntity<Map<String, String>> = ResponseEntity.status(status).body(mapOf("error" to code))
}
