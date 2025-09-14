package com.gaarx.tvplayer.data

import com.gaarx.tvplayer.data.services.SettingsService
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

    suspend fun updateLastChannelLoaded(channelId: Long) {
        settingsService.updateLastChannelLoaded(channelId)
    }

    suspend fun getLastChannelLoaded(): Long {
        return settingsService.getLastChannelLoaded()
    }

    suspend fun updateLastCategoryLoaded(categoryId: Long) {
        settingsService.updateLastCategoryLoaded(categoryId)
    }

    suspend fun getLastCategoryLoaded(): Long {
        return settingsService.getLastCategoryLoaded()
    }

    suspend fun updateEpgSource(epgSource: String) {
        settingsService.updateEpgSource(epgSource)
    }

    suspend fun getEpgSource() : String {
        return settingsService.getEpgSource()
    }

    suspend fun getConfigURL(): String {
        return settingsService.getConfigURL()
    }

    suspend fun updateConfigURL(url: String) {
        settingsService.updateConfigURL(url)
    }
}