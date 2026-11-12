package app.ilecto.proho.net

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The JSON shapes in api/openapi/openapi.yaml (Home, MyUnit, AdministrationSummary). */
class HomeParsingTest {
    private val community =
        """{"id":"c1","key":"los-robles","name":"Los Robles","legalName":null,"taxId":null,"kind":"ph_residential",
           "plan":null,"country":"CO","currency":"COP","timezone":"America/Bogota","address":"Calle 1 # 2-3"}"""

    @Test
    fun `an administrator's home has counts`() {
        val home =
            JSONObject(
                """{"community":$community,"membershipId":"m1","personId":null,"roles":["administrator"],"units":[],
                   "administration":{"buildings":1,"units":12,"people":30,"members":8,"suspendedMembers":1,"pendingInvitations":2}}""",
            ).toHome()

        assertEquals("Los Robles", home.community.name)
        assertEquals("Calle 1 # 2-3", home.community.address)
        assertNull(home.personId)
        assertTrue(home.isAdministrator)
        assertEquals(AdministrationSummaryDto(1, 12, 30, 8, 1, 2), home.administration)
    }

    @Test
    fun `a member's home has their units and no counts`() {
        val home =
            JSONObject(
                """{"community":$community,"membershipId":"m2","personId":"p1","roles":[],
                   "units":[{"unitId":"u1","number":"P-7","kind":"parking","building":null,"roles":["tenant"]},
                            {"unitId":"u2","number":"201","kind":"apartment","building":"Torre 2","roles":["owner","resident"]}],
                   "administration":null}""",
            ).toHome()

        assertFalse(home.isAdministrator)
        assertNull(home.administration)
        assertEquals(
            listOf(
                MyUnitDto("u1", "P-7", "parking", null, listOf("tenant")),
                MyUnitDto("u2", "201", "apartment", "Torre 2", listOf("owner", "resident")),
            ),
            home.units,
        )
    }

    @Test
    fun `fields the caller can't see come back null`() {
        val person =
            JSONObject(
                """{"id":"p1","communityId":"c1","kind":"natural","givenName":"Ana","surname":"Gómez","legalName":null,
                   "idType":null,"idNumber":null,"email":null,"phone":null,"nameVisibility":"community",
                   "emailVisibility":"privileged","phoneVisibility":"privileged","unitsVisibility":"privileged","archivedAt":null}""",
            ).toPerson()

        assertEquals("Ana Gómez", person.displayName)
        assertNull(person.email)
    }
}
