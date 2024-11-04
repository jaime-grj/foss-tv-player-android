package com.gaarj.iptvplayer.domain

import com.gaarj.iptvplayer.data.EPGRepository
import com.gaarj.iptvplayer.data.SettingsRepository
import com.gaarj.iptvplayer.domain.model.ChannelItem
import javax.inject.Inject


class UpdateEPGUseCase @Inject constructor(
    private val epgRepository: EPGRepository,
    private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(channelList: List<ChannelItem>) {
        for (channel in channelList) {
            channel.epgPrograms = epgRepository.getEPGProgramsForChannel(channel.id)
        }
    }
}