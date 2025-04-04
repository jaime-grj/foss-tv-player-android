package com.gaarx.iptvplayer.domain

import com.gaarx.iptvplayer.data.EPGRepository
import com.gaarx.iptvplayer.data.SettingsRepository
import javax.inject.Inject


class GetSettingsUseCase @Inject constructor(
    private val epgRepository: EPGRepository,
    private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke() : Long {

        val lastDownloadedTime = settingsRepository.getLastDownloadedTime()
        //println("lastDownloadedTime: $lastDownloadedTime, current: ${System.currentTimeMillis() - lastDownloadedTime}")
        return lastDownloadedTime

    }
}