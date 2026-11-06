package net.interlecto.proho.identity

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class RegisterAccountRequest(
    @field:NotBlank @field:Email @field:Size(max = 320)
    val email: String,
    // BCrypt (the default delegate) silently ignores bytes past 72; reject
    // longer passwords instead of truncating them without telling anyone.
    @field:NotBlank @field:Size(min = 8, max = 72)
    val password: String,
    @field:NotBlank @field:Size(max = 200)
    val displayName: String,
    @field:Size(max = 10)
    val locale: String? = null,
)

data class UpdateAccountRequest(
    @field:NotBlank @field:Size(max = 200)
    val displayName: String,
    @field:NotBlank @field:Size(max = 10)
    val locale: String,
)

data class AccountResponse(
    val id: UUID,
    val email: String,
    val displayName: String,
    val locale: String,
    val status: String,
)

// Internal, not private: AuthController reuses this to build LoginResponse.account.
internal fun Account.toResponse() = AccountResponse(id, email, displayName, locale, status.value)

@RestController
class AccountController(
    private val registrationService: AccountRegistrationService,
    private val profileService: AccountProfileService,
) {
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterAccountRequest,
    ): AccountResponse {
        val account =
            registrationService.register(
                RegisterAccountCommand(
                    email = request.email,
                    password = request.password,
                    displayName = request.displayName,
                    locale = request.locale ?: "es-CO",
                ),
            )
        return account.toResponse()
    }

    // See docs/architecture/domain-model.md §10.1: "The API exposes, in /me
    // and per community, which modules and capabilities are enabled."
    // This slice returns only account fields; growing it to include enabled
    // modules/capabilities is later work once the `communities` module exists.
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): AccountResponse =
        AccountResponse(
            id = account.accountId,
            email = account.email,
            displayName = account.displayName,
            locale = account.locale,
            status = account.status.value,
        )

    @PutMapping("/me")
    fun updateMe(
        @Valid @RequestBody request: UpdateAccountRequest,
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ): AccountResponse = profileService.update(account.accountId, request.displayName, request.locale).toResponse()

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun handleDuplicateEmail(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "email_already_registered"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
