package com.gaarj.iptvplayer.domain

import com.gaarj.iptvplayer.data.EPGRepository
import com.gaarj.iptvplayer.data.SettingsRepository
import javax.inject.Inject


class UpdateEPGUseCase @Inject constructor(
    private val epgRepository: EPGRepository,
    private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke() {

        val lastDownloadedTime = settingsRepository.getLastDownloadedTime()
        println("lastDownloadedTime: $lastDownloadedTime")
        if (lastDownloadedTime == 0L || System.currentTimeMillis() - lastDownloadedTime > 24 * 60 * 60 * 1000) {
            epgRepository.downloadEPG()
            settingsRepository.updateLastDownloadedTime(System.currentTimeMillis())
        }

    }
}