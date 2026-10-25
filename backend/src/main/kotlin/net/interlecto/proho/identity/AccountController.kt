package net.interlecto.proho.identity

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
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

data class AccountResponse(
    val id: UUID,
    val email: String,
    val displayName: String,
    val locale: String,
    val status: String,
)

private fun Account.toResponse() = AccountResponse(id, email, displayName, locale, status.value)

@RestController
class AccountController(private val registrationService: AccountRegistrationService) {
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

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun handleDuplicateEmail(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "email_already_registered"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
