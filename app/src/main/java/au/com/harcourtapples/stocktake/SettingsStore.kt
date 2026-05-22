package au.com.harcourtapples.stocktake

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prefs")

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL  = stringPreferencesKey("server_url")
        private val KEY_API_KEY     = stringPreferencesKey("api_key")
        private val KEY_OFFLINE     = booleanPreferencesKey("offline_mode")
        const val DEFAULT_URL = "http://192.168.1.100:5050"
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: DEFAULT_URL
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val offlineMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OFFLINE] ?: false
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url.trimEnd('/') }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key.trim() }
    }

    suspend fun setOfflineMode(offline: Boolean) {
        context.dataStore.edit { it[KEY_OFFLINE] = offline }
    }
}
