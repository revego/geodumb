package com.code4you.geodumb

// SharedPreferencesHelper.kt
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Date

class SharedPreferencesHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"

        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FACEBOOK_ID = "facebook_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_TOKEN_EXPIRY = "token_expirysaveImageRecordId"
        private const val KEY_IMAGE_RECORDS = "image_records_simple"
    }

    // Salva il token JWT
    fun saveAuthToken(token: String) {
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            // IMPOSTA LA SCADENZA (30 minuti da ora)
            val expiryTime = System.currentTimeMillis() + (30 * 60 * 1000)
            putLong(KEY_TOKEN_EXPIRY, expiryTime)
            apply()

            Log.d("SharedPrefs", "Token salvato con scadenza: ${Date(expiryTime)}")
        }
        //Log.d("SharedPrefs", "Token salvato con scadenza: ${Date(expiryTime)}")
        //prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    // Recupera il token JWT
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    // Salva tutti i dati dell'utente
    fun saveUserData(
        token: String,
        userId: String,
        facebookId: String? = null,
        email: String? = null
    ) {
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_USER_ID, userId)
            facebookId?.let { putString(KEY_FACEBOOK_ID, it) }
            email?.let { putString(KEY_EMAIL, it) }
            // Salva il timestamp di scadenza (30 minuti da ora)
            val expiryTime = System.currentTimeMillis() + (30 * 60 * 1000)
            putLong(KEY_TOKEN_EXPIRY, expiryTime)
        }.apply()
    }

    // Verifica se il token è valido (non scaduto)
    fun isTokenValid(): Boolean {
        val expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        return expiryTime > System.currentTimeMillis() && getAuthToken() != null
    }

    // Recupera tutti i dati utente
    fun getUserData(): UserData {
        return UserData(
            token = prefs.getString(KEY_AUTH_TOKEN, null),
            userId = prefs.getString(KEY_USER_ID, null),
            facebookId = prefs.getString(KEY_FACEBOOK_ID, null),
            email = prefs.getString(KEY_EMAIL, null)
        )
    }

    // Cancella tutti i dati (logout)
    fun clearUserData() {
        prefs.edit().clear().apply()
    }

    data class UserData(
        val token: String?,
        val userId: String?,
        val facebookId: String?,
        val email: String?
    )

    fun saveImageRecord(imagePath: String, recordId: Int) {
        val fileName = imagePath.substringAfterLast("/")
        val imageKey = fileName.hashCode().toString()

        // Formato: "key1:id1,key2:id2,..."
        val currentRecords = prefs.getString(KEY_IMAGE_RECORDS, "") ?: ""
        val recordsMap = parseSimpleRecords(currentRecords).toMutableMap()
        recordsMap[imageKey] = recordId

        val newRecords = recordsMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        prefs.edit().putString(KEY_IMAGE_RECORDS, newRecords).apply()

        Log.d("SharedPrefs", "Salvato record $recordId per $fileName")
    }

    fun getImageRecordId(imagePath: String): Int? {
        val fileName = imagePath.substringAfterLast("/")
        val imageKey = fileName.hashCode().toString()

        val currentRecords = prefs.getString(KEY_IMAGE_RECORDS, "") ?: ""
        val recordsMap = parseSimpleRecords(currentRecords)

        return recordsMap[imageKey]
    }

    fun removeImageRecord(imagePath: String) {
        val fileName = imagePath.substringAfterLast("/")
        val imageKey = fileName.hashCode().toString()

        val currentRecords = prefs.getString(KEY_IMAGE_RECORDS, "") ?: ""
        val recordsMap = parseSimpleRecords(currentRecords).toMutableMap()
        recordsMap.remove(imageKey)

        val newRecords = recordsMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        prefs.edit().putString(KEY_IMAGE_RECORDS, newRecords).apply()
    }

    fun getAllImageRecords(): Map<String, Int> {
        val currentRecords = prefs.getString(KEY_IMAGE_RECORDS, "") ?: ""
        return parseSimpleRecords(currentRecords)
    }

    private fun parseSimpleRecords(recordsString: String): Map<String, Int> {
        if (recordsString.isEmpty()) return emptyMap()

        return try {
            recordsString.split(",")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val id = parts[1].toIntOrNull()
                        if (id != null && id > 0) {
                            parts[0] to id
                        } else null
                    } else null
                }
                .toMap()
        } catch (e: Exception) {
            Log.e("SharedPrefs", "Errore parsing: ${e.message}")
            emptyMap()
        }
    }
}