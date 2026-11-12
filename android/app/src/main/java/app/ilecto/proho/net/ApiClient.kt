package app.ilecto.proho.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// The dev backend is reached via `adb reverse tcp:8080 tcp:8080`, which
// makes the device's own localhost forward to the host machine's --
// works for both a physical device over USB and the emulator. See
// docs/architecture/client-screens.md.
private const val API_BASE = "http://127.0.0.1:8080"

class ApiException(val status: Int, val code: String) : Exception(code)

data class AccountDto(
    val id: String,
    val email: String,
    val displayName: String,
    val status: String,
)

data class CommunityDto(
    val id: String,
    val key: String,
    val name: String,
    val kind: String,
    val address: String? = null,
)

/** One of the caller's own units today. */
data class MyUnitDto(
    val unitId: String,
    val number: String,
    val kind: String,
    val building: String?,
    val roles: List<String>,
)

data class AdministrationSummaryDto(
    val buildings: Int,
    val units: Int,
    val people: Int,
    val members: Int,
    val suspendedMembers: Int,
    val pendingInvitations: Int,
)

/** GET /communities/{id}/home: the dashboard, the same on web and iOS. */
data class HomeDto(
    val community: CommunityDto,
    val membershipId: String,
    val personId: String?,
    val roles: List<String>,
    val units: List<MyUnitDto>,
    val administration: AdministrationSummaryDto?,
) {
    val isAdministrator: Boolean get() = "administrator" in roles
}

data class UnitDto(
    val id: String,
    val number: String,
    val kind: String,
    val buildingId: String?,
    val floor: String?,
)

/** A person as the caller may see them: fields they can't see come back null. */
data class PersonDto(
    val id: String,
    val kind: String,
    val givenName: String?,
    val surname: String?,
    val legalName: String?,
    val email: String?,
    val phone: String?,
) {
    val displayName: String
        get() = if (kind == "legal") legalName.orEmpty() else listOfNotNull(givenName, surname).joinToString(" ")
}

data class RelationshipDto(
    val id: String,
    val unitId: String,
    val personId: String,
    val role: String,
    val share: String?,
    val validFrom: String,
    val validTo: String?,
)

data class LoginResult(val token: String, val account: AccountDto)

object ApiClient {
    suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): AccountDto =
        withContext(Dispatchers.IO) {
            val body =
                JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("displayName", displayName)
            call("POST", "/accounts", body, null).toAccount()
        }

    suspend fun login(
        email: String,
        password: String,
    ): LoginResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("email", email).put("password", password)
            val response = call("POST", "/auth/login", body, null)
            LoginResult(response.getString("token"), response.getJSONObject("account").toAccount())
        }

    suspend fun me(token: String): AccountDto =
        withContext(Dispatchers.IO) {
            call("GET", "/me", null, token).toAccount()
        }

    suspend fun logout(token: String) {
        withContext(Dispatchers.IO) {
            call("POST", "/auth/logout", null, token)
        }
    }

    suspend fun listCommunities(token: String): List<CommunityDto> =
        withContext(Dispatchers.IO) {
            callArray("GET", "/communities", null, token).objects().map { it.toCommunity() }
        }

    suspend fun createCommunity(
        token: String,
        key: String,
        name: String,
    ): CommunityDto =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("key", key).put("name", name).put("kind", "ph_residential")
            call("POST", "/communities", body, token).toCommunity()
        }

    suspend fun getHome(
        token: String,
        communityId: String,
    ): HomeDto = withContext(Dispatchers.IO) { call("GET", "/communities/$communityId/home", null, token).toHome() }

    suspend fun listUnits(
        token: String,
        communityId: String,
    ): List<UnitDto> =
        withContext(Dispatchers.IO) {
            callArray("GET", "/communities/$communityId/units", null, token).objects().map { it.toUnit() }
        }

    suspend fun listUnitRelationships(
        token: String,
        communityId: String,
        unitId: String,
    ): List<RelationshipDto> =
        withContext(Dispatchers.IO) {
            callArray("GET", "/communities/$communityId/units/$unitId/relationships", null, token)
                .objects()
                .map { it.toRelationship() }
        }

    suspend fun listPeople(
        token: String,
        communityId: String,
    ): List<PersonDto> =
        withContext(Dispatchers.IO) {
            callArray("GET", "/communities/$communityId/people", null, token).objects().map { it.toPerson() }
        }

    /** The caller's own record, or null when no person is linked to their membership. */
    suspend fun getProfile(
        token: String,
        communityId: String,
    ): PersonDto? =
        withContext(Dispatchers.IO) {
            try {
                call("GET", "/communities/$communityId/profile", null, token).toPerson()
            } catch (e: ApiException) {
                if (e.code == "profile_not_linked") null else throw e
            }
        }

    suspend fun createUnit(
        token: String,
        communityId: String,
        number: String,
        kind: String,
    ): UnitDto =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("number", number).put("kind", kind)
            call("POST", "/communities/$communityId/units", body, token).toUnit()
        }

    suspend fun createPerson(
        token: String,
        communityId: String,
        givenName: String,
        surname: String,
    ): PersonDto =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("kind", "natural").put("givenName", givenName).put("surname", surname)
            call("POST", "/communities/$communityId/people", body, token).toPerson()
        }

    suspend fun createRelationship(
        token: String,
        communityId: String,
        unitId: String,
        personId: String,
        role: String,
    ): RelationshipDto =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("personId", personId).put("role", role)
            call("POST", "/communities/$communityId/units/$unitId/relationships", body, token).toRelationship()
        }

    private fun call(
        method: String,
        path: String,
        body: JSONObject?,
        token: String?,
    ): JSONObject {
        val text = raw(method, path, body, token)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun callArray(
        method: String,
        path: String,
        body: JSONObject?,
        token: String?,
    ): JSONArray {
        val text = raw(method, path, body, token)
        return if (text.isBlank()) JSONArray() else JSONArray(text)
    }

    private fun raw(
        method: String,
        path: String,
        body: JSONObject?,
        token: String?,
    ): String {
        val connection = URL(API_BASE + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Content-Type", "application/json")
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            connection.doInput = true
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (status !in 200..299) {
                val code =
                    runCatching { JSONObject(text).optString("error", "unknown_error") }
                        .getOrDefault("unknown_error")
                throw ApiException(status, code)
            }
            return text
        } finally {
            connection.disconnect()
        }
    }
}

internal fun JSONObject.stringOrNull(name: String): String? = if (isNull(name)) null else getString(name)

internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

internal fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

internal fun JSONObject.toAccount() =
    AccountDto(
        id = getString("id"),
        email = getString("email"),
        displayName = getString("displayName"),
        status = getString("status"),
    )

internal fun JSONObject.toCommunity() =
    CommunityDto(
        id = getString("id"),
        key = getString("key"),
        name = getString("name"),
        kind = getString("kind"),
        address = stringOrNull("address"),
    )

internal fun JSONObject.toHome() =
    HomeDto(
        community = getJSONObject("community").toCommunity(),
        membershipId = getString("membershipId"),
        personId = stringOrNull("personId"),
        roles = getJSONArray("roles").strings(),
        units =
            getJSONArray("units").objects().map {
                MyUnitDto(
                    unitId = it.getString("unitId"),
                    number = it.getString("number"),
                    kind = it.getString("kind"),
                    building = it.stringOrNull("building"),
                    roles = it.getJSONArray("roles").strings(),
                )
            },
        administration =
            if (isNull("administration")) {
                null
            } else {
                getJSONObject("administration").let {
                    AdministrationSummaryDto(
                        buildings = it.getInt("buildings"),
                        units = it.getInt("units"),
                        people = it.getInt("people"),
                        members = it.getInt("members"),
                        suspendedMembers = it.getInt("suspendedMembers"),
                        pendingInvitations = it.getInt("pendingInvitations"),
                    )
                }
            },
    )

internal fun JSONObject.toUnit() =
    UnitDto(
        id = getString("id"),
        number = getString("number"),
        kind = getString("kind"),
        buildingId = stringOrNull("buildingId"),
        floor = stringOrNull("floor"),
    )

internal fun JSONObject.toPerson() =
    PersonDto(
        id = getString("id"),
        kind = getString("kind"),
        givenName = stringOrNull("givenName"),
        surname = stringOrNull("surname"),
        legalName = stringOrNull("legalName"),
        email = stringOrNull("email"),
        phone = stringOrNull("phone"),
    )

internal fun JSONObject.toRelationship() =
    RelationshipDto(
        id = getString("id"),
        unitId = getString("unitId"),
        personId = getString("personId"),
        role = getString("role"),
        share = stringOrNull("share"),
        validFrom = getString("validFrom"),
        validTo = stringOrNull("validTo"),
    )
