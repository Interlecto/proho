package app.ilecto.proho.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ilecto.proho.R
import app.ilecto.proho.net.AccountDto
import app.ilecto.proho.net.ApiClient
import app.ilecto.proho.net.PersonDto
import kotlinx.coroutines.launch

private sealed interface ProfileState {
    data object Loading : ProfileState

    data object Unlinked : ProfileState

    data object Failed : ProfileState

    data class Ready(val person: PersonDto) : ProfileState
}

/** The caller's account and their own record in this community. Editing what they share is on the web. */
@Composable
fun ProfileScreen(
    token: String,
    communityId: String,
    account: AccountDto,
    onSwitchCommunity: () -> Unit,
    onLogout: () -> Unit,
) {
    var state by remember { mutableStateOf<ProfileState>(ProfileState.Loading) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token, communityId) {
        state =
            runCatching { ApiClient.getProfile(token, communityId) }
                .fold(
                    onSuccess = { it?.let(ProfileState::Ready) ?: ProfileState.Unlinked },
                    onFailure = { ProfileState.Failed },
                )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.profile_account_title), style = MaterialTheme.typography.titleMedium)
        Text(account.displayName)
        Text(account.email, style = MaterialTheme.typography.bodySmall)

        Text(stringResource(R.string.profile_community_title), style = MaterialTheme.typography.titleMedium)
        when (val current = state) {
            ProfileState.Loading -> Text(stringResource(R.string.loading))
            ProfileState.Unlinked -> Text(stringResource(R.string.home_not_linked))
            ProfileState.Failed -> Text(stringResource(R.string.error_load), color = MaterialTheme.colorScheme.error)
            is ProfileState.Ready -> {
                Text(current.person.displayName)
                listOfNotNull(current.person.email, current.person.phone).forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(stringResource(R.string.profile_edit_on_web), style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedButton(onClick = onSwitchCommunity, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.switch_community))
        }
        TextButton(onClick = {
            scope.launch { runCatching { ApiClient.logout(token) } }
            onLogout()
        }) {
            Text(stringResource(R.string.logout_action))
        }
    }
}
