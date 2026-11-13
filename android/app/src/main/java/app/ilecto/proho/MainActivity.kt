package app.ilecto.proho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ilecto.proho.net.AccountDto
import app.ilecto.proho.net.ApiClient
import app.ilecto.proho.ui.screens.AuthScreen
import app.ilecto.proho.ui.screens.CommunitiesScreen
import app.ilecto.proho.ui.screens.CommunityScreen
import app.ilecto.proho.ui.theme.ProHoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProHoTheme {
                ProHoApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProHoApp() {
    // Demo-only session: not persisted across app restarts, unlike the web
    // client's localStorage token. See docs/architecture/client-screens.md.
    var token by remember { mutableStateOf<String?>(null) }
    var account by remember { mutableStateOf<AccountDto?>(null) }
    var checkingSession by remember { mutableStateOf(false) }
    var communityId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        val currentToken = token
        if (currentToken == null) {
            account = null
            return@LaunchedEffect
        }
        checkingSession = true
        account = runCatching { ApiClient.me(currentToken) }.getOrNull()
        if (account == null) {
            token = null
            communityId = null
        }
        checkingSession = false
    }

    val signedInToken = token
    val signedInAccount = account
    val openCommunity = communityId
    if (signedInToken != null && signedInAccount != null && openCommunity != null) {
        // A community has its own top and bottom bars (ui/navigation/Section.kt).
        key(openCommunity) {
            CommunityScreen(
                token = signedInToken,
                account = signedInAccount,
                communityId = openCommunity,
                onSwitch = { communityId = it },
                onLeave = { communityId = null },
                onLogout = {
                    communityId = null
                    token = null
                },
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            stringResource(R.string.tagline),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentToken = token
            val currentAccount = account
            when {
                checkingSession -> Text(stringResource(R.string.loading), modifier = Modifier.padding(16.dp))
                currentToken != null && currentAccount != null ->
                    CommunitiesScreen(
                        token = currentToken,
                        account = currentAccount,
                        onOpen = { communityId = it },
                        onLogout = { token = null },
                    )
                else -> AuthScreen(onLoggedIn = { newToken -> token = newToken })
            }
        }
    }
}
