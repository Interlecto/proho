package app.ilecto.proho.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.ilecto.proho.R

// Labels for codes the API returns, the same wording as the web client's
// web/src/labels.ts. An unknown code is shown as it is.

val UNIT_KINDS: Map<String, Int> =
    linkedMapOf(
        "apartment" to R.string.kind_apartment,
        "house" to R.string.kind_house,
        "commercial" to R.string.kind_commercial,
        "office" to R.string.kind_office,
        "parking" to R.string.kind_parking,
        "storage" to R.string.kind_storage,
    )

val RELATIONSHIP_ROLES: Map<String, Int> =
    linkedMapOf(
        "owner" to R.string.relationship_owner,
        "tenant" to R.string.relationship_tenant,
        "resident" to R.string.relationship_resident,
        "household_member" to R.string.relationship_household_member,
        "representative" to R.string.relationship_representative,
    )

val ASSIGNED_ROLES: Map<String, Int> =
    linkedMapOf(
        "administrator" to R.string.role_administrator,
        "accountant" to R.string.role_accountant,
        "treasurer" to R.string.role_treasurer,
        "auditor" to R.string.role_auditor,
        "council_member" to R.string.role_council_member,
        "moderator" to R.string.role_moderator,
        "staff" to R.string.role_staff,
    )

private val ERRORS: Map<String, Int> =
    mapOf(
        "unit_number_already_registered" to R.string.error_unit_number_already_registered,
        "person_id_already_registered" to R.string.error_person_id_already_registered,
        "owner_share_exceeds_100" to R.string.error_owner_share_exceeds_100,
        "role_not_held" to R.string.error_role_not_held,
        "membership_not_active" to R.string.error_membership_not_active,
        "invalid_request" to R.string.error_invalid_request,
    )

@StringRes
fun errorMessage(code: String?): Int = ERRORS[code] ?: R.string.error_unknown

@Composable
fun label(
    labels: Map<String, Int>,
    code: String,
): String = labels[code]?.let { stringResource(it) } ?: code
