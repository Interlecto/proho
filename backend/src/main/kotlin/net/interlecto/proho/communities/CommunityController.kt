package net.interlecto.proho.communities

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import net.interlecto.proho.identity.AuthenticatedAccount
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateCommunityRequest(
    @field:NotBlank @field:Size(max = 64) @field:Pattern(regexp = "^[a-z0-9-]+$")
    val key: String,
    @field:NotBlank @field:Size(max = 200)
    val name: String,
    @field:Size(max = 200)
    val legalName: String? = null,
    @field:Size(max = 32)
    val taxId: String? = null,
    @field:NotBlank @field:Size(max = 40)
    val kind: String,
    @field:Size(max = 40)
    val plan: String? = null,
    @field:Size(min = 2, max = 2)
    val country: String? = null,
    @field:Size(min = 3, max = 3)
    val currency: String? = null,
    @field:Size(max = 64)
    val timezone: String? = null,
    val address: String? = null,
)

data class CommunityResponse(
    val id: UUID,
    val key: String,
    val name: String,
    val legalName: String?,
    val taxId: String?,
    val kind: String,
    val plan: String?,
    val country: String,
    val currency: String,
    val timezone: String,
    val address: String?,
)

internal fun Community.toResponse() =
    CommunityResponse(id, key, name, legalName, taxId, kind, plan, country, currency, timezone, address)

@RestController
class CommunityController(
    private val communityService: CommunityService,
    private val communities: CommunityRepository,
    private val memberships: MembershipRepository,
) {
    @PostMapping("/communities")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateCommunityRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): CommunityResponse {
        val community =
            communityService.create(
                CreateCommunityCommand(
                    key = request.key,
                    name = request.name,
                    legalName = request.legalName,
                    taxId = request.taxId,
                    kind = request.kind,
                    plan = request.plan,
                    country = request.country ?: "CO",
                    currency = request.currency ?: "COP",
                    timezone = request.timezone ?: "America/Bogota",
                    address = request.address,
                ),
                account.accountId,
            )
        return community.toResponse()
    }

    @GetMapping("/communities")
    fun mine(
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): List<CommunityResponse> {
        val communityIds =
            memberships.findByAccountIdAndStatusNot(account.accountId, MembershipStatus.ENDED)
                .filter { it.status == MembershipStatus.ACTIVE }
                .map { it.communityId }
        return communities.findAllById(communityIds).map { it.toResponse() }
    }

    @ExceptionHandler(CommunityKeyAlreadyRegisteredException::class)
    fun handleDuplicateKey(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "community_key_already_registered"))

    @ExceptionHandler(UnsupportedCommunityKindException::class)
    fun handleUnsupportedKind(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "unsupported_community_kind"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
