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
            val array = callArray("GET", "/communities", null, token)
            (0 until array.length()).map { array.getJSONObject(it).toCommunity() }
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

private fun JSONObject.toAccount() =
    AccountDto(
        id = getString("id"),
        email = getString("email"),
        displayName = getString("displayName"),
        status = getString("status"),
    )

private fun JSONObject.toCommunity() =
    CommunityDto(
        id = getString("id"),
        key = getString("key"),
        name = getString("name"),
        kind = getString("kind"),
    )
