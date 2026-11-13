package app.ilecto.proho.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ilecto.proho.R
import app.ilecto.proho.net.AccountDto
import app.ilecto.proho.net.ApiClient
import app.ilecto.proho.net.ApiException
import app.ilecto.proho.net.CommunityDto
import app.ilecto.proho.net.HomeDto
import app.ilecto.proho.net.UnitDto
import app.ilecto.proho.ui.navigation.BackStep
import app.ilecto.proho.ui.navigation.Section
import app.ilecto.proho.ui.navigation.backStep

/**
 * One community: a switcher in the top bar, the shared sections in the bottom
 * bar, and an add button where an administrator can add something.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    token: String,
    account: AccountDto,
    communityId: String,
    onSwitch: (String) -> Unit,
    onLeave: () -> Unit,
    onLogout: () -> Unit,
) {
    var section by rememberSaveable { mutableStateOf(Section.HOME) }
    var home by remember { mutableStateOf<HomeDto?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var communities by remember { mutableStateOf<List<CommunityDto>>(emptyList()) }
    var openUnit by remember { mutableStateOf<UnitDto?>(null) }
    var adding by remember { mutableStateOf(false) }
    // Bumped after a change, so Home's counts and the lists load again.
    var version by remember { mutableIntStateOf(0) }

    LaunchedEffect(token, communityId, version) {
        loadFailed = false
        try {
            home = ApiClient.getHome(token, communityId)
        } catch (e: ApiException) {
            if (e.code == "membership_not_active") onLeave() else loadFailed = true
        } catch (e: Exception) {
            loadFailed = true
        }
    }
    LaunchedEffect(token) {
        communities = runCatching { ApiClient.listCommunities(token) }.getOrDefault(emptyList())
    }

    BackHandler {
        when (backStep(section, openUnit != null)) {
            BackStep.CloseDetail -> openUnit = null
            BackStep.GoHome -> section = Section.HOME
            BackStep.LeaveCommunity -> onLeave()
        }
    }

    val current = home
    val canAdd = current?.isAdministrator == true && openUnit == null && section in setOf(Section.UNITS, Section.DIRECTORY)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    CommunitySwitcher(
                        name = current?.community?.name ?: stringResource(R.string.loading),
                        communities = communities,
                        currentId = communityId,
                        onSwitch = onSwitch,
                        onAll = onLeave,
                    )
                },
                navigationIcon = {
                    if (openUnit != null) {
                        TextButton(onClick = { openUnit = null }) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back_action),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        },
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = entry == section,
                        onClick = {
                            openUnit = null
                            if (entry == Section.HOME) version++
                            section = entry
                        },
                        icon = { Icon(painterResource(entry.icon), contentDescription = null) },
                        label = { Text(stringResource(entry.label)) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(onClick = { adding = true }) {
                    Icon(
                        painterResource(R.drawable.ic_add),
                        contentDescription =
                            stringResource(if (section == Section.UNITS) R.string.add_unit else R.string.add_person),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                current == null && loadFailed -> LoadFailed(onRetry = { version++ })
                current == null -> Text(stringResource(R.string.loading), modifier = Modifier.padding(16.dp))
                else ->
                    when (section) {
                        Section.HOME -> HomeScreen(current)
                        Section.UNITS ->
                            UnitsScreen(
                                token = token,
                                communityId = communityId,
                                isAdministrator = current.isAdministrator,
                                version = version,
                                openUnit = openUnit,
                                onOpenUnit = { openUnit = it },
                                adding = adding,
                                onAddingDone = { changed ->
                                    adding = false
                                    if (changed) version++
                                },
                            )
                        Section.DIRECTORY ->
                            DirectoryScreen(
                                token = token,
                                communityId = communityId,
                                version = version,
                                adding = adding,
                                onAddingDone = { changed ->
                                    adding = false
                                    if (changed) version++
                                },
                            )
                        Section.PROFILE ->
                            ProfileScreen(
                                token = token,
                                communityId = communityId,
                                account = account,
                                onSwitchCommunity = onLeave,
                                onLogout = onLogout,
                            )
                    }
            }
        }
    }
}

@Composable
private fun CommunitySwitcher(
    name: String,
    communities: List<CommunityDto>,
    currentId: String,
    onSwitch: (String) -> Unit,
    onAll: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                Icon(
                    painterResource(R.drawable.ic_arrow_drop_down),
                    contentDescription = stringResource(R.string.switch_community),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            communities.filter { it.id != currentId }.forEach { community ->
                DropdownMenuItem(
                    text = { Text(community.name) },
                    onClick = {
                        expanded = false
                        onSwitch(community.id)
                    },
                )
            }
            if (communities.size > 1) HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_communities)) },
                onClick = {
                    expanded = false
                    onAll()
                },
            )
        }
    }
}

@Composable
fun LoadFailed(onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.error_load), color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.retry_action))
        }
    }
}
