package app.ilecto.proho.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.ilecto.proho.R

/**
 * The sections every member sees, in the same order as the web and iOS
 * clients (docs/architecture/client-screens.md). The web's administration
 * sections (members, buildings, community details, history) stay on the web;
 * an administrator gets the trimmed actions inside Units and Directory.
 */
enum class Section(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    HOME(R.string.section_home, R.drawable.ic_home),
    UNITS(R.string.section_units, R.drawable.ic_units),
    DIRECTORY(R.string.section_directory, R.drawable.ic_people),
    PROFILE(R.string.section_profile, R.drawable.ic_person),
}

/** What the back button does: leave a detail, then go to Home, then leave the community. */
sealed interface BackStep {
    data object CloseDetail : BackStep

    data object GoHome : BackStep

    data object LeaveCommunity : BackStep
}

fun backStep(
    section: Section,
    detailOpen: Boolean,
): BackStep =
    when {
        detailOpen -> BackStep.CloseDetail
        section != Section.HOME -> BackStep.GoHome
        else -> BackStep.LeaveCommunity
    }
