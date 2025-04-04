package com.gaarx.iptvplayer.domain

import com.gaarx.iptvplayer.data.EPGRepository
import com.gaarx.iptvplayer.domain.model.EPGProgramItem
import com.gaarx.iptvplayer.domain.model.toDomain
import javax.inject.Inject

class GetNextProgramUseCase @Inject  constructor(private val epgRepository: EPGRepository) {

    suspend operator fun invoke(channelId : Long): EPGProgramItem? {
        return epgRepository.getNextProgramForChannel(channelId)?.toDomain()
    }
}