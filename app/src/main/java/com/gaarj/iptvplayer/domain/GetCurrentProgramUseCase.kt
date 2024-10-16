package com.gaarj.iptvplayer.domain

import com.gaarj.iptvplayer.data.EPGRepository
import com.gaarj.iptvplayer.domain.model.EPGProgramItem
import com.gaarj.iptvplayer.domain.model.toDomain
import javax.inject.Inject

class GetCurrentProgramUseCase @Inject  constructor(private val epgRepository: EPGRepository) {

    suspend operator fun invoke(channelId : Long): EPGProgramItem? {
        return epgRepository.getCurrentProgramForChannel(channelId)?.toDomain()
    }
}