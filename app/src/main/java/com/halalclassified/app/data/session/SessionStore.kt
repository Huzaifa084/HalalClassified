package com.halalclassified.app.data.session

import android.content.Context
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class StoredSession(
    val userId: String,
    val email: String,
    val name: String,
    val session: UserSession
)

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadSessions(): List<StoredSession> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StoredSession.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    fun loadAccounts(): List<StoredAccount> = loadSessions().map {
        StoredAccount(id = it.userId, name = it.name, email = it.email)
    }

    fun getSession(userId: String): StoredSession? = loadSessions().firstOrNull { it.userId == userId }

    fun saveSession(session: UserSession) {
        val user = session.user ?: return
        val email = user.email ?: return
        val storedSession = StoredSession(
            userId = user.id,
            email = email,
            name = deriveName(user) ?: email,
            session = session
        )
        val sessions = loadSessions().toMutableList().apply {
            removeAll { it.userId == storedSession.userId }
            add(0, storedSession)
        }
        saveSessions(sessions)
    }

    fun removeSession(userId: String) {
        val sessions = loadSessions().filterNot { it.userId == userId }
        saveSessions(sessions)
    }

    private fun saveSessions(sessions: List<StoredSession>) {
        val raw = json.encodeToString(ListSerializer(StoredSession.serializer()), sessions)
        prefs.edit().putString(KEY_SESSIONS, raw).apply()
    }

    private fun deriveName(user: UserInfo): String? {
        val metadata = user.userMetadata ?: return null
        val fullName = metadata["full_name"].asStringOrNull()
        if (!fullName.isNullOrBlank()) return fullName
        val firstName = metadata["first_name"].asStringOrNull()
        val lastName = metadata["last_name"].asStringOrNull()
        return listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { null }
    }

    private fun JsonElement?.asStringOrNull(): String? = this?.jsonPrimitive?.contentOrNull

    private companion object {
        const val PREFS_NAME = "halal_classified_sessions"
        const val KEY_SESSIONS = "stored_sessions"
    }
}
