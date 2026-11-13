package app.ilecto.proho.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class SectionTest {
    @Test
    fun `the sections match the web and iOS order`() {
        assertEquals(listOf(Section.HOME, Section.UNITS, Section.DIRECTORY, Section.PROFILE), Section.entries)
    }

    @Test
    fun `back closes a detail, then goes home, then leaves the community`() {
        assertEquals(BackStep.CloseDetail, backStep(Section.UNITS, detailOpen = true))
        assertEquals(BackStep.GoHome, backStep(Section.DIRECTORY, detailOpen = false))
        assertEquals(BackStep.LeaveCommunity, backStep(Section.HOME, detailOpen = false))
    }
}
