package net.interlecto.proho.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.launch
import net.interlecto.proho.R
import net.interlecto.proho.net.AccountDto
import net.interlecto.proho.net.ApiClient
import net.interlecto.proho.net.ApiException
import net.interlecto.proho.net.CommunityDto

@Composable
fun CommunitiesScreen(
    token: String,
    account: AccountDto,
    onLogout: () -> Unit,
) {
    var communities by remember { mutableStateOf<List<CommunityDto>?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token, refreshKey) {
        communities = runCatching { ApiClient.listCommunities(token) }.getOrNull() ?: emptyList()
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AssistChip(onClick = {}, label = { Text(account.displayName) })
            TextButton(onClick = {
                scope.launch { runCatching { ApiClient.logout(token) } }
                onLogout()
            }) {
                Text(stringResource(R.string.logout_action))
            }
        }
        Text(stringResource(R.string.your_communities_title), style = MaterialTheme.typography.headlineSmall)

        when {
            communities == null -> Text(stringResource(R.string.loading))
            communities!!.isEmpty() && !showCreate -> Text(stringResource(R.string.no_communities_yet))
            else ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(communities!!) { community ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(community.name, style = MaterialTheme.typography.titleMedium)
                                Text(community.key, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
        }

        if (!showCreate) {
            Button(onClick = { showCreate = true }) {
                Text(stringResource(R.string.create_community_action))
            }
        } else {
            CreateCommunityForm(
                token = token,
                onCreated = {
                    showCreate = false
                    refreshKey++
                },
                onCancel = { showCreate = false },
            )
        }
    }
}

@Composable
private fun CreateCommunityForm(
    token: String,
    onCreated: () -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val duplicateKeyError = stringResource(R.string.error_community_key_already_registered)
    val unknownError = stringResource(R.string.error_unknown)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.community_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it.lowercase() },
            label = { Text(stringResource(R.string.community_key_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = !busy,
                onClick = {
                    error = null
                    busy = true
                    scope.launch {
                        try {
                            ApiClient.createCommunity(token, key, name)
                            onCreated()
                        } catch (e: ApiException) {
                            error = if (e.code == "community_key_already_registered") duplicateKeyError else unknownError
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.create_action))
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    }
}
