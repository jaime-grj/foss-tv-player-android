package com.gaarj.iptvplayer.data

import com.gaarj.iptvplayer.data.services.SettingsService
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val settingsService: SettingsService
) {

    suspend fun updateLastDownloadedTime(time: Long) {
        settingsService.updateLastDownloadedTime(time)
    }

    suspend fun getLastDownloadedTime(): Long {
        return settingsService.getLastDownloadedTime()
    }
}