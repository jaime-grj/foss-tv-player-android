package com.gaarj.iptvplayer.data.services
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsService(private val context: Context) {

    companion object {
        private const val EPG_LAST_DOWNLOADED_TIME = "lastDownloadedTime"
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    suspend fun updateLastDownloadedTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[longPreferencesKey(EPG_LAST_DOWNLOADED_TIME)] = time
        }
    }

    suspend fun getLastDownloadedTime(): Long {
        return context.dataStore.data.map { preferences ->
            preferences[longPreferencesKey(EPG_LAST_DOWNLOADED_TIME)] ?: 0
        }.first()
    }
}