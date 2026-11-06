package net.interlecto.proho.residents

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import net.interlecto.proho.communities.MembershipNotActiveException
import net.interlecto.proho.communities.MembershipNotFoundException
import net.interlecto.proho.communities.PersonAlreadyLinkedException
import net.interlecto.proho.communities.RoleNotHeldException
import net.interlecto.proho.identity.AuthenticatedAccount
import net.interlecto.proho.properties.UnitNotFoundException
import net.interlecto.proho.shared.RecordArchivedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreatePersonRequest(
    @field:NotBlank @field:Size(max = 10)
    val kind: String,
    @field:Size(max = 120)
    val givenName: String? = null,
    @field:Size(max = 120)
    val surname: String? = null,
    @field:Size(max = 200)
    val legalName: String? = null,
    @field:Size(max = 10)
    val idType: String? = null,
    @field:Size(max = 40)
    val idNumber: String? = null,
    @field:Email @field:Size(max = 320)
    val email: String? = null,
    @field:Size(max = 40)
    val phone: String? = null,
)

private fun CreatePersonRequest.toDetails() = PersonDetails(kind, givenName, surname, legalName, idType, idNumber, email, phone)

data class UpdateProfileRequest(
    @field:Email @field:Size(max = 320)
    val email: String? = null,
    @field:Size(max = 40)
    val phone: String? = null,
    @field:NotBlank
    val nameVisibility: String,
    @field:NotBlank
    val emailVisibility: String,
    @field:NotBlank
    val phoneVisibility: String,
    @field:NotBlank
    val unitsVisibility: String,
)

data class LinkPersonRequest(val personId: UUID? = null)

data class PersonResponse(
    val id: UUID,
    val communityId: UUID,
    val kind: String,
    val givenName: String?,
    val surname: String?,
    val legalName: String?,
    val idType: String?,
    val idNumber: String?,
    val email: String?,
    val phone: String?,
    val nameVisibility: String,
    val emailVisibility: String,
    val phoneVisibility: String,
    val unitsVisibility: String,
    val archivedAt: Instant?,
)

private fun Person.toResponse() =
    PersonResponse(
        id,
        communityId,
        kind,
        givenName,
        surname,
        legalName,
        idType,
        idNumber,
        email,
        phone,
        nameVisibility,
        emailVisibility,
        phoneVisibility,
        unitsVisibility,
        archivedAt,
    )

data class CreateUnitRelationshipRequest(
    @field:NotNull
    val personId: UUID,
    @field:NotBlank @field:Size(max = 20)
    val role: String,
    @field:DecimalMin("0") @field:DecimalMax("100") @field:Digits(integer = 3, fraction = 4)
    val share: BigDecimal? = null,
    val validFrom: LocalDate? = null,
    val validTo: LocalDate? = null,
)

data class EndUnitRelationshipRequest(val validTo: LocalDate? = null)

data class UnitRelationshipResponse(
    val id: UUID,
    val unitId: UUID,
    val personId: UUID,
    val role: String,
    val share: BigDecimal?,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
)

private fun UnitRelationship.toResponse() = UnitRelationshipResponse(id, unitId, personId, role, share, validFrom, validTo)

// A PUT replaces the same fields a POST sets, so both take the same request.
@RestController
class ResidentController(
    private val residentService: ResidentService,
) {
    @PostMapping("/communities/{communityId}/people")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPerson(
        @PathVariable communityId: UUID,
        @Valid @RequestBody request: CreatePersonRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): PersonResponse = residentService.createPerson(communityId, request.toDetails(), account.accountId).toResponse()

    @GetMapping("/communities/{communityId}/people")
    fun listPeople(
        @PathVariable communityId: UUID,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<PersonResponse> =
        residentService.listPeople(communityId, includeArchived, account.accountId).map { it.toResponse() }

    @GetMapping("/communities/{communityId}/people/{personId}")
    fun getPerson(
        @PathVariable communityId: UUID,
        @PathVariable personId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): PersonResponse = residentService.getPerson(communityId, personId, account.accountId).toResponse()

    @PutMapping("/communities/{communityId}/people/{personId}")
    fun updatePerson(
        @PathVariable communityId: UUID,
        @PathVariable personId: UUID,
        @Valid @RequestBody request: CreatePersonRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): PersonResponse =
        residentService.updatePerson(communityId, personId, request.toDetails(), account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/people/{personId}/archive")
    fun archivePerson(
        @PathVariable communityId: UUID,
        @PathVariable personId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): PersonResponse = residentService.archivePerson(communityId, personId, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/people/{personId}/restore")
    fun restorePerson(
        @PathVariable communityId: UUID,
        @PathVariable personId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): PersonResponse = residentService.restorePerson(communityId, personId, account.accountId).toResponse()

    @GetMapping("/communities/{communityId}/profile")
    fun getProfile(
        @PathVariable communityId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): PersonResponse = residentService.getProfile(communityId, account.accountId).toResponse()

    @PutMapping("/communities/{communityId}/profile")
    fun updateProfile(
        @PathVariable communityId: UUID,
        @Valid @RequestBody request: UpdateProfileRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): PersonResponse =
        residentService.updateProfile(
            communityId,
            ProfileDetails(
                request.email,
                request.phone,
                request.nameVisibility,
                request.emailVisibility,
                request.phoneVisibility,
                request.unitsVisibility,
            ),
            account.accountId,
        ).toResponse()

    @PutMapping("/communities/{communityId}/memberships/{membershipId}/person")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun linkMembership(
        @PathVariable communityId: UUID,
        @PathVariable membershipId: UUID,
        @RequestBody request: LinkPersonRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ) = residentService.linkMembership(communityId, membershipId, request.personId, account.accountId)

    @GetMapping("/communities/{communityId}/people/{personId}/relationships")
    fun listPersonRelationships(
        @PathVariable communityId: UUID,
        @PathVariable personId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<UnitRelationshipResponse> =
        residentService.listPersonRelationships(communityId, personId, account.accountId).map { it.toResponse() }

    @PostMapping("/communities/{communityId}/units/{unitId}/relationships")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUnitRelationship(
        @PathVariable communityId: UUID,
        @PathVariable unitId: UUID,
        @Valid @RequestBody request: CreateUnitRelationshipRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): UnitRelationshipResponse =
        residentService.createUnitRelationship(
            communityId = communityId,
            unitId = unitId,
            personId = request.personId,
            role = request.role,
            share = request.share,
            validFrom = request.validFrom,
            validTo = request.validTo,
            accountId = account.accountId,
        ).toResponse()

    @GetMapping("/communities/{communityId}/units/{unitId}/relationships")
    fun listUnitRelationships(
        @PathVariable communityId: UUID,
        @PathVariable unitId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<UnitRelationshipResponse> =
        residentService.listUnitRelationships(communityId, unitId, account.accountId).map { it.toResponse() }

    @PostMapping("/communities/{communityId}/units/{unitId}/relationships/{relationshipId}/end")
    fun endUnitRelationship(
        @PathVariable communityId: UUID,
        @PathVariable unitId: UUID,
        @PathVariable relationshipId: UUID,
        @RequestBody(required = false) request: EndUnitRelationshipRequest?,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): UnitRelationshipResponse =
        residentService.endUnitRelationship(communityId, unitId, relationshipId, request?.validTo, account.accountId)
            .toResponse()

    @ExceptionHandler(
        PersonNotFoundException::class,
        RelationshipNotFoundException::class,
        UnitNotFoundException::class,
        MembershipNotFoundException::class,
    )
    fun handleNotFound(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "not_found_in_community"))

    @ExceptionHandler(ProfileNotLinkedException::class)
    fun handleProfileNotLinked(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "profile_not_linked"))

    @ExceptionHandler(PersonAlreadyLinkedException::class)
    fun handlePersonAlreadyLinked(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "person_already_linked"))

    @ExceptionHandler(PersonInUseException::class)
    fun handlePersonInUse(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "person_in_use"))

    @ExceptionHandler(RelationshipAlreadyEndedException::class)
    fun handleRelationshipAlreadyEnded(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "relationship_already_ended"))

    @ExceptionHandler(RecordArchivedException::class)
    fun handleRecordArchived(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "record_archived"))

    @ExceptionHandler(MembershipNotActiveException::class)
    fun handleMembershipNotActive(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "membership_not_active"))

    @ExceptionHandler(RoleNotHeldException::class)
    fun handleRoleNotHeld(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "role_not_held"))

    @ExceptionHandler(PersonIdAlreadyRegisteredException::class)
    fun handleDuplicatePersonId(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "person_id_already_registered"))

    @ExceptionHandler(OwnerShareExceeds100Exception::class)
    fun handleOwnerShareExceeds100(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "owner_share_exceeds_100"))

    @ExceptionHandler(
        UnsupportedPersonKindException::class,
        InvalidPersonNameException::class,
        InvalidPersonIdException::class,
        UnsupportedVisibilityException::class,
        UnsupportedRelationshipRoleException::class,
        InvalidRelationshipPeriodException::class,
    )
    fun handleInvalidResidentRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))

    @ExceptionHandler(PersonNotInCommunityException::class)
    fun handleCrossCommunityReference(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "not_found_in_community"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
