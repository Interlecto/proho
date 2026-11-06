package net.interlecto.proho.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import net.interlecto.proho.communities.MembershipNotActiveException
import net.interlecto.proho.communities.RoleNotHeldException
import net.interlecto.proho.identity.AuthenticatedAccount
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
import java.util.UUID

data class CreateBuildingRequest(
    @field:NotBlank @field:Size(max = 200)
    val name: String,
)

data class BuildingResponse(val id: UUID, val communityId: UUID, val name: String, val archivedAt: Instant?)

private fun Building.toResponse() = BuildingResponse(id, communityId, name, archivedAt)

data class CreateUnitRequest(
    @field:NotBlank @field:Size(max = 40)
    val number: String,
    @field:NotBlank @field:Size(max = 20)
    val kind: String,
    val buildingId: UUID? = null,
    @field:Size(max = 20)
    val floor: String? = null,
    @field:DecimalMin("0") @field:Digits(integer = 8, fraction = 2)
    val areaM2: BigDecimal? = null,
    @field:Size(max = 60)
    val registryId: String? = null,
)

private fun CreateUnitRequest.toDetails() = UnitDetails(number, kind, buildingId, floor, areaM2, registryId)

data class UnitResponse(
    val id: UUID,
    val communityId: UUID,
    val buildingId: UUID?,
    val number: String,
    val kind: String,
    val floor: String?,
    val areaM2: BigDecimal?,
    val registryId: String?,
    val archivedAt: Instant?,
)

private fun PropertyUnit.toResponse() =
    UnitResponse(id, communityId, buildingId, number, kind, floor, areaM2, registryId, archivedAt)

// A PUT replaces the same fields a POST sets, so both take the same request.
@RestController
class PropertyController(
    private val propertyService: PropertyService,
) {
    @PostMapping("/communities/{communityId}/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBuilding(
        @PathVariable communityId: UUID,
        @Valid @RequestBody request: CreateBuildingRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): BuildingResponse =
        propertyService.createBuilding(communityId, request.name, account.accountId).toResponse()

    @GetMapping("/communities/{communityId}/buildings")
    fun listBuildings(
        @PathVariable communityId: UUID,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<BuildingResponse> =
        propertyService.listBuildings(communityId, includeArchived, account.accountId).map { it.toResponse() }

    @GetMapping("/communities/{communityId}/buildings/{buildingId}")
    fun getBuilding(
        @PathVariable communityId: UUID,
        @PathVariable buildingId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): BuildingResponse = propertyService.getBuilding(communityId, buildingId, account.accountId).toResponse()

    @PutMapping("/communities/{communityId}/buildings/{buildingId}")
    fun updateBuilding(
        @PathVariable communityId: UUID,
        @PathVariable buildingId: UUID,
        @Valid @RequestBody request: CreateBuildingRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): BuildingResponse =
        propertyService.updateBuilding(communityId, buildingId, request.name, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/buildings/{buildingId}/archive")
    fun archiveBuilding(
        @PathVariable communityId: UUID,
        @PathVariable buildingId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): BuildingResponse = propertyService.archiveBuilding(communityId, buildingId, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/buildings/{buildingId}/restore")
    fun restoreBuilding(
        @PathVariable communityId: UUID,
        @PathVariable buildingId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): BuildingResponse = propertyService.restoreBuilding(communityId, buildingId, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/units")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUnit(
        @PathVariable communityId: UUID,
        @Valid @RequestBody request: CreateUnitRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): UnitResponse = propertyService.createUnit(communityId, request.toDetails(), account.accountId).toResponse()

    @GetMapping("/communities/{communityId}/units")
    fun listUnits(
        @PathVariable communityId: UUID,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<UnitResponse> =
        propertyService.listUnits(communityId, includeArchived, account.accountId).map { it.toResponse() }

    @GetMapping("/communities/{communityId}/units/{unitId}")
    fun getUnit(
        @PathVariable communityId: UUID,
        @PathVariable unitId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): UnitResponse = propertyService.getUnit(communityId, unitId, account.accountId).toResponse()

    @PutMapping("/communities/{communityId}/units/{unitId}")
    fun updateUnit(
        @PathVariable communityId: UUID,
        @PathVariable unitId: UUID,
        @Valid @RequestBody request: CreateUnitRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): UnitResponse =
        propertyService.updateUnit(communityId, unitId, request.toDetails(), account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/units/{unitId}/archive")
    fun archiveUnit(
        @PathVariable communityId: UUID,
        @PathVariable unitId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): UnitResponse = propertyService.archiveUnit(communityId, unitId, account.accountId).toResponse()

    @PostMapping("/communities/{communityId}/units/{unitId}/restore")
    fun restoreUnit(
        @PathVariable communityId: UUID,
        @PathVariable unitId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): UnitResponse = propertyService.restoreUnit(communityId, unitId, account.accountId).toResponse()

    @ExceptionHandler(BuildingNotFoundException::class, UnitNotFoundException::class)
    fun handleNotFound(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "not_found_in_community"))

    @ExceptionHandler(BuildingInUseException::class)
    fun handleBuildingInUse(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "building_in_use"))

    @ExceptionHandler(UnitInUseException::class)
    fun handleUnitInUse(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "unit_in_use"))

    @ExceptionHandler(RecordArchivedException::class)
    fun handleRecordArchived(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "record_archived"))

    @ExceptionHandler(MembershipNotActiveException::class)
    fun handleMembershipNotActive(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "membership_not_active"))

    @ExceptionHandler(RoleNotHeldException::class)
    fun handleRoleNotHeld(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "role_not_held"))

    @ExceptionHandler(UnitNumberAlreadyRegisteredException::class)
    fun handleDuplicateUnitNumber(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "unit_number_already_registered"))

    @ExceptionHandler(UnsupportedUnitKindException::class)
    fun handleUnsupportedUnitKind(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "unsupported_unit_kind"))

    @ExceptionHandler(BuildingNotInCommunityException::class)
    fun handleCrossCommunityReference(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "not_found_in_community"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
