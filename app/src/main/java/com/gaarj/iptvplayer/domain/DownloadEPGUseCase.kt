package com.gaarj.iptvplayer.domain

import com.gaarj.iptvplayer.data.EPGRepository
import com.gaarj.iptvplayer.data.SettingsRepository
import com.gaarj.iptvplayer.domain.model.ChannelItem
import javax.inject.Inject


class DownloadEPGUseCase @Inject constructor(
    private val epgRepository: EPGRepository) {

    suspend operator fun invoke() {
        epgRepository.downloadEPG()
    }
}