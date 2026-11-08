package app.ilecto.proho.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import app.ilecto.proho.R
import app.ilecto.proho.net.ApiClient
import app.ilecto.proho.net.ApiException

private enum class AuthMode { LOGIN, REGISTER }

@Composable
fun AuthScreen(onLoggedIn: (String) -> Unit) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Resolved here (composable context) so the plain-Kotlin catch block
    // below can map an API error code to one without calling stringResource
    // outside composition.
    val errInvalidCredentials = stringResource(R.string.error_invalid_credentials)
    val errEmailAlreadyRegistered = stringResource(R.string.error_email_already_registered)
    val errInvalidRequest = stringResource(R.string.error_invalid_request)
    val errUnknown = stringResource(R.string.error_unknown)

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AssistChip(onClick = {}, label = { Text(stringResource(R.string.no_community)) })
        Text(
            stringResource(if (mode == AuthMode.LOGIN) R.string.login_title else R.string.register_title),
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
        )
        if (mode == AuthMode.REGISTER) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.name_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy,
            onClick = {
                error = null
                busy = true
                scope.launch {
                    try {
                        if (mode == AuthMode.REGISTER) {
                            ApiClient.register(email, password, displayName)
                        }
                        val result = ApiClient.login(email, password)
                        onLoggedIn(result.token)
                    } catch (e: ApiException) {
                        error =
                            when (e.code) {
                                "invalid_credentials" -> errInvalidCredentials
                                "email_already_registered" -> errEmailAlreadyRegistered
                                "invalid_request" -> errInvalidRequest
                                else -> errUnknown
                            }
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        busy = false
                    }
                }
            },
        ) {
            Text(stringResource(if (mode == AuthMode.LOGIN) R.string.login_title else R.string.register_title))
        }
        TextButton(onClick = { mode = if (mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN }) {
            Text(stringResource(if (mode == AuthMode.LOGIN) R.string.no_account_prompt else R.string.has_account_prompt))
        }
    }
}
