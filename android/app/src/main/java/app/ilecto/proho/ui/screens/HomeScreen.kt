package app.ilecto.proho.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ilecto.proho.R
import app.ilecto.proho.net.AdministrationSummaryDto
import app.ilecto.proho.net.HomeDto
import app.ilecto.proho.ui.ASSIGNED_ROLES
import app.ilecto.proho.ui.RELATIONSHIP_ROLES
import app.ilecto.proho.ui.UNIT_KINDS
import app.ilecto.proho.ui.label

/** The dashboard: the same blocks, in the same order, as the web and iOS. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(home: HomeDto) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        home.community.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (home.roles.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                home.roles.forEach { role -> AssistChip(onClick = {}, label = { Text(label(ASSIGNED_ROLES, role)) }) }
            }
        }

        Text(stringResource(R.string.my_units_title), style = MaterialTheme.typography.titleMedium)
        when {
            home.personId == null -> Text(stringResource(R.string.home_not_linked))
            home.units.isEmpty() -> Text(stringResource(R.string.home_no_units))
            else ->
                home.units.forEach { unit ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                listOfNotNull(unit.building, unit.number).joinToString(" · "),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(label(UNIT_KINDS, unit.kind), style = MaterialTheme.typography.bodySmall)
                            Text(unit.roles.map { label(RELATIONSHIP_ROLES, it) }.joinToString(", "))
                        }
                    }
                }
        }
        Text(stringResource(R.string.home_billing_placeholder), style = MaterialTheme.typography.bodySmall)

        home.administration?.let { AdministrationBlock(it) }
    }
}

@Composable
private fun AdministrationBlock(summary: AdministrationSummaryDto) {
    Text(stringResource(R.string.administration_title), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CountCard(stringResource(R.string.count_units), summary.units, Modifier.weight(1f))
        CountCard(stringResource(R.string.count_people), summary.people, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CountCard(stringResource(R.string.count_buildings), summary.buildings, Modifier.weight(1f))
        CountCard(
            stringResource(R.string.count_members),
            summary.members,
            Modifier.weight(1f),
            detail =
                listOfNotNull(
                    if (summary.suspendedMembers > 0) stringResource(R.string.members_suspended, summary.suspendedMembers) else null,
                    if (summary.pendingInvitations > 0) stringResource(R.string.invitations_pending, summary.pendingInvitations) else null,
                ).joinToString(" · "),
        )
    }
    Text(stringResource(R.string.admin_on_web), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun CountCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    detail: String = "",
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label)
            if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}
