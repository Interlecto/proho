package app.ilecto.proho.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import app.ilecto.proho.net.RelationshipDto
import app.ilecto.proho.net.UnitDto
import app.ilecto.proho.ui.RELATIONSHIP_ROLES
import app.ilecto.proho.ui.UNIT_KINDS
import app.ilecto.proho.ui.errorMessage
import app.ilecto.proho.ui.label
import kotlinx.coroutines.launch

/** The community's units, read-only for members. An administrator adds units and relationships. */
@Composable
fun UnitsScreen(
    token: String,
    communityId: String,
    isAdministrator: Boolean,
    version: Int,
    openUnit: UnitDto?,
    onOpenUnit: (UnitDto) -> Unit,
    adding: Boolean,
    onAddingDone: (changed: Boolean) -> Unit,
) {
    var units by remember { mutableStateOf<List<UnitDto>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(token, communityId, version, retry) {
        failed = false
        runCatching { ApiClient.listUnits(token, communityId) }
            .onSuccess { units = it.sortedBy(UnitDto::number) }
            .onFailure { failed = true }
    }

    if (openUnit != null) {
        UnitDetail(token, communityId, openUnit, isAdministrator)
        return
    }

    val loaded = units
    when {
        loaded == null && failed -> LoadFailed(onRetry = { retry++ })
        loaded == null -> Text(stringResource(R.string.loading), modifier = Modifier.padding(16.dp))
        loaded.isEmpty() -> Text(stringResource(R.string.no_units), modifier = Modifier.padding(16.dp))
        else ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(loaded, key = UnitDto::id) { unit ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenUnit(unit) }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(unit.number, style = MaterialTheme.typography.titleMedium)
                            Text(label(UNIT_KINDS, unit.kind), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
    }

    if (adding) CreateUnitDialog(token, communityId, onDone = onAddingDone)
}

@Composable
private fun UnitDetail(
    token: String,
    communityId: String,
    unit: UnitDto,
    isAdministrator: Boolean,
) {
    var relationships by remember { mutableStateOf<List<RelationshipDto>?>(null) }
    var people by remember { mutableStateOf<Map<String, PersonDto>>(emptyMap()) }
    var addingRelationship by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(unit.id, reload) {
        failed = false
        runCatching {
            relationships = ApiClient.listUnitRelationships(token, communityId, unit.id)
            people = ApiClient.listPeople(token, communityId).associateBy(PersonDto::id)
        }.onFailure { failed = true }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(unit.number, style = MaterialTheme.typography.headlineSmall)
        Text(label(UNIT_KINDS, unit.kind))
        unit.floor?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text(stringResource(R.string.unit_relationships_title), style = MaterialTheme.typography.titleMedium)
        val loaded = relationships
        when {
            loaded == null && failed -> LoadFailed(onRetry = { reload++ })
            loaded == null -> Text(stringResource(R.string.loading))
            loaded.isEmpty() -> Text(stringResource(R.string.no_relationships))
            else ->
                loaded.forEach { relationship ->
                    val name = people[relationship.personId]?.displayName?.ifBlank { null }
                    Text(
                        "${name ?: stringResource(R.string.person_hidden)} · ${label(RELATIONSHIP_ROLES, relationship.role)}" +
                            (relationship.share?.let { " · $it %" } ?: "") +
                            (relationship.validTo?.let { " · ${relationship.validFrom} – $it" } ?: ""),
                    )
                }
        }
        if (isAdministrator) {
            OutlinedButton(onClick = { addingRelationship = true }) {
                Text(stringResource(R.string.add_relationship))
            }
        }
    }

    if (addingRelationship) {
        AddRelationshipDialog(
            token = token,
            communityId = communityId,
            unitId = unit.id,
            people = people.values.sortedBy(PersonDto::displayName),
            onDone = { changed ->
                addingRelationship = false
                if (changed) reload++
            },
        )
    }
}

@Composable
private fun CreateUnitDialog(
    token: String,
    communityId: String,
    onDone: (Boolean) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("apartment") }
    var error by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { onDone(false) },
        title = { Text(stringResource(R.string.add_unit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.unit_number_label)) },
                    singleLine = true,
                )
                Choice(
                    label = stringResource(R.string.unit_kind_label),
                    options = UNIT_KINDS.keys.toList(),
                    selected = kind,
                    optionLabel = { label(UNIT_KINDS, it) },
                    onSelect = { kind = it },
                )
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && number.isNotBlank(),
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            ApiClient.createUnit(token, communityId, number.trim(), kind)
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

@Composable
private fun AddRelationshipDialog(
    token: String,
    communityId: String,
    unitId: String,
    people: List<PersonDto>,
    onDone: (Boolean) -> Unit,
) {
    var personId by remember { mutableStateOf<String?>(null) }
    var role by remember { mutableStateOf("resident") }
    var error by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val names = people.associate { it.id to it.displayName }

    AlertDialog(
        onDismissRequest = { onDone(false) },
        title = { Text(stringResource(R.string.add_relationship)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(
                    label = stringResource(R.string.person_label),
                    options = people.map(PersonDto::id),
                    selected = personId,
                    optionLabel = { names[it].orEmpty() },
                    onSelect = { personId = it },
                )
                Choice(
                    label = stringResource(R.string.relationship_role_label),
                    options = RELATIONSHIP_ROLES.keys.toList(),
                    selected = role,
                    optionLabel = { label(RELATIONSHIP_ROLES, it) },
                    onSelect = { role = it },
                )
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && personId != null,
                onClick = {
                    val chosen = personId ?: return@Button
                    busy = true
                    scope.launch {
                        try {
                            ApiClient.createRelationship(token, communityId, unitId, chosen, role)
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

/** A labelled button that opens a menu of options. */
@Composable
fun Choice(
    label: String,
    options: List<String>,
    selected: String?,
    optionLabel: @Composable (String) -> String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.let { optionLabel(it) } ?: stringResource(R.string.choose_option))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}
