package com.gaarx.tvplayer.data.services

import com.gaarx.tvplayer.data.DataApi
import android.util.Log
import javax.inject.Inject

class DataService @Inject constructor(
    private val settingsService: SettingsService,
    private val api: DataApi
) {
    suspend fun getJSONString(): String {
        return try {
            api.getJson(settingsService.getConfigURL())
        } catch (e: Exception) {
            Log.e("DataService", "Network error", e)
            ""
        }
    }
}
