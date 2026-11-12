package app.ilecto.proho.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ilecto.proho.R
import app.ilecto.proho.net.ApiClient
import app.ilecto.proho.net.ApiException
import app.ilecto.proho.net.PersonDto
import app.ilecto.proho.ui.errorMessage
import kotlinx.coroutines.launch

/**
 * The people the caller may see (docs/security/privacy.md): by default only
 * those who share their name, and those on the caller's own units. The
 * backend does the filtering; this only shows what comes back.
 */
@Composable
fun DirectoryScreen(
    token: String,
    communityId: String,
    version: Int,
    adding: Boolean,
    onAddingDone: (changed: Boolean) -> Unit,
) {
    var people by remember { mutableStateOf<List<PersonDto>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(token, communityId, version, retry) {
        failed = false
        runCatching { ApiClient.listPeople(token, communityId) }
            .onSuccess { people = it.filter { person -> person.displayName.isNotBlank() }.sortedBy(PersonDto::displayName) }
            .onFailure { failed = true }
    }

    val loaded = people
    when {
        loaded == null && failed -> LoadFailed(onRetry = { retry++ })
        loaded == null -> Text(stringResource(R.string.loading), modifier = Modifier.padding(16.dp))
        loaded.isEmpty() -> Text(stringResource(R.string.no_people), modifier = Modifier.padding(16.dp))
        else ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(loaded, key = PersonDto::id) { person ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(person.displayName, style = MaterialTheme.typography.titleMedium)
                            listOfNotNull(person.email, person.phone).forEach {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
    }

    if (adding) CreatePersonDialog(token, communityId, onDone = onAddingDone)
}

@Composable
private fun CreatePersonDialog(
    token: String,
    communityId: String,
    onDone: (Boolean) -> Unit,
) {
    var givenName by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { onDone(false) },
        title = { Text(stringResource(R.string.add_person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = givenName,
                    onValueChange = { givenName = it },
                    label = { Text(stringResource(R.string.given_name_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text(stringResource(R.string.surname_label)) },
                    singleLine = true,
                )
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && givenName.isNotBlank() && surname.isNotBlank(),
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            ApiClient.createPerson(token, communityId, givenName.trim(), surname.trim())
                            onDone(true)
                        } catch (e: ApiException) {
                            error = errorMessage(e.code)
                        } catch (e: Exception) {
                            error = R.string.error_unknown
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(stringResource(R.string.save_action)) }
        },
        dismissButton = { TextButton(onClick = { onDone(false) }) { Text(stringResource(R.string.cancel_action)) } },
    )
}
