package net.interlecto.proho.residents

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import net.interlecto.proho.communities.MembershipNotActiveException
import net.interlecto.proho.communities.RoleNotHeldException
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
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
)

data class PersonResponse(
    val id: UUID,
    val communityId: UUID,
    val kind: String,
    val givenName: String?,
    val surname: String?,
    val legalName: String?,
    val idType: String?,
    val idNumber: String?,
)

private fun Person.toResponse() = PersonResponse(id, communityId, kind, givenName, surname, legalName, idType, idNumber)

data class CreateUnitRelationshipRequest(
    @field:NotNull
    val personId: UUID,
    @field:NotBlank @field:Size(max = 20)
    val role: String,
    val share: BigDecimal? = null,
    val validFrom: LocalDate? = null,
    val validTo: LocalDate? = null,
)

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
    ): PersonResponse =
        residentService.createPerson(
            communityId = communityId,
            kind = request.kind,
            givenName = request.givenName,
            surname = request.surname,
            legalName = request.legalName,
            idType = request.idType,
            idNumber = request.idNumber,
            accountId = account.accountId,
        ).toResponse()

    @GetMapping("/communities/{communityId}/people")
    fun listPeople(
        @PathVariable communityId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<PersonResponse> = residentService.listPeople(communityId, account.accountId).map { it.toResponse() }

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

    @ExceptionHandler(UnsupportedPersonKindException::class, InvalidPersonNameException::class, UnsupportedRelationshipRoleException::class)
    fun handleInvalidResidentRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))

    @ExceptionHandler(UnitNotInCommunityException::class, PersonNotInCommunityException::class)
    fun handleCrossCommunityReference(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "not_found_in_community"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
