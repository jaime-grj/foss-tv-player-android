package com.gaarx.tvplayer.data.services
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsService(private val context: Context) {

    companion object {
        private const val EPG_LAST_DOWNLOADED_TIME = "lastDownloadedTime"
        private const val LAST_CHANNEL_LOADED = "lastChannelLoaded"
        private const val LAST_CATEGORY_LOADED = "lastCategoryLoaded"
        private const val EPG_SOURCES = "epgSources"
        private const val CONFIG_URL = "configURL"
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

    suspend fun updateLastChannelLoaded(channelId: Long) {
        context.dataStore.edit { preferences ->
            preferences[longPreferencesKey(LAST_CHANNEL_LOADED)] = channelId
        }
    }

    suspend fun getLastChannelLoaded(): Long {
        return context.dataStore.data.map { preferences ->
            preferences[longPreferencesKey(LAST_CHANNEL_LOADED)] ?: 0
        }.first()
    }

    suspend fun updateLastCategoryLoaded(categoryId: Long) {
        context.dataStore.edit { preferences ->
            preferences[longPreferencesKey(LAST_CATEGORY_LOADED)] = categoryId
        }
    }

    suspend fun getLastCategoryLoaded(): Long {
        return context.dataStore.data.map { preferences ->
            preferences[longPreferencesKey(LAST_CATEGORY_LOADED)] ?: 0
        }.first()
    }

    suspend fun updateEpgSource(epgSource: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(EPG_SOURCES)] = epgSource
        }
    }

    suspend fun getEpgSource(): String {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(EPG_SOURCES)] ?: ""
        }.first()
    }

    suspend fun getConfigURL(): String {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(CONFIG_URL)] ?: "http://filehost.zhnx.home.arpa/channels.json"
        }.first()
    }

    suspend fun updateConfigURL(newUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(CONFIG_URL)] = newUrl
        }
    }
}