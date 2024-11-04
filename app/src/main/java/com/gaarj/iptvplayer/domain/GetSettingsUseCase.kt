package com.gaarj.iptvplayer.domain

import com.gaarj.iptvplayer.data.EPGRepository
import com.gaarj.iptvplayer.data.SettingsRepository
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