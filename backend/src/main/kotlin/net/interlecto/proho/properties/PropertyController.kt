package net.interlecto.proho.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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
import java.util.UUID

data class CreateBuildingRequest(
    @field:NotBlank @field:Size(max = 200)
    val name: String,
)

data class BuildingResponse(val id: UUID, val communityId: UUID, val name: String)

private fun Building.toResponse() = BuildingResponse(id, communityId, name)

data class CreateUnitRequest(
    @field:NotBlank @field:Size(max = 40)
    val number: String,
    @field:NotBlank @field:Size(max = 20)
    val kind: String,
    val buildingId: UUID? = null,
    @field:Size(max = 20)
    val floor: String? = null,
    val areaM2: BigDecimal? = null,
    @field:Size(max = 60)
    val registryId: String? = null,
)

data class UnitResponse(
    val id: UUID,
    val communityId: UUID,
    val buildingId: UUID?,
    val number: String,
    val kind: String,
    val floor: String?,
    val areaM2: BigDecimal?,
    val registryId: String?,
)

private fun PropertyUnit.toResponse() =
    UnitResponse(id, communityId, buildingId, number, kind, floor, areaM2, registryId)

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
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<BuildingResponse> = propertyService.listBuildings(communityId, account.accountId).map { it.toResponse() }

    @PostMapping("/communities/{communityId}/units")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUnit(
        @PathVariable communityId: UUID,
        @Valid @RequestBody request: CreateUnitRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): UnitResponse =
        propertyService.createUnit(
            communityId = communityId,
            number = request.number,
            kind = request.kind,
            buildingId = request.buildingId,
            floor = request.floor,
            areaM2 = request.areaM2,
            registryId = request.registryId,
            accountId = account.accountId,
        ).toResponse()

    @GetMapping("/communities/{communityId}/units")
    fun listUnits(
        @PathVariable communityId: UUID,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<UnitResponse> = propertyService.listUnits(communityId, account.accountId).map { it.toResponse() }

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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
