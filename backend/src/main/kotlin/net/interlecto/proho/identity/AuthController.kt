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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class LoginRequest(
    @field:NotBlank @field:Email @field:Size(max = 320)
    val email: String,
    @field:NotBlank @field:Size(max = 72)
    val password: String,
)

data class LoginResponse(
    val token: String,
    val expiresAt: Instant,
    val account: AccountResponse,
)

data class RequestPasswordResetRequest(
    @field:NotBlank @field:Email @field:Size(max = 320)
    val email: String,
)

data class ConfirmPasswordResetRequest(
    @field:NotBlank
    val token: String,
    // Same bcrypt-truncation reasoning as RegisterAccountRequest.password.
    @field:NotBlank @field:Size(min = 8, max = 72)
    val newPassword: String,
)

class InvalidOrExpiredTokenException : RuntimeException("Invalid or expired password reset token")

@RestController
class AuthController(
    private val authenticationService: AuthenticationService,
    private val passwordResetService: PasswordResetService,
) {
    @PostMapping("/auth/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): LoginResponse {
        val result = authenticationService.login(request.email, request.password)
        return LoginResponse(
            token = result.token,
            expiresAt = result.expiresAt,
            account = result.account.toResponse(),
        )
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @AuthenticationPrincipal account: AuthenticatedAccount,
    ) {
        authenticationService.logout(account.sessionId)
    }

    @PostMapping("/auth/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestPasswordReset(
        @Valid @RequestBody request: RequestPasswordResetRequest,
    ) {
        passwordResetService.request(request.email)
    }

    @PostMapping("/auth/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun confirmPasswordReset(
        @Valid @RequestBody request: ConfirmPasswordResetRequest,
    ) {
        val succeeded = passwordResetService.confirm(request.token, request.newPassword)
        if (!succeeded) {
            throw InvalidOrExpiredTokenException()
        }
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "invalid_credentials"))

    @ExceptionHandler(InvalidOrExpiredTokenException::class)
    fun handleInvalidOrExpiredToken(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_or_expired_token"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to "invalid_request"))
}
