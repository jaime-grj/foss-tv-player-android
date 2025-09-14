package com.gaarx.tvplayer.domain

import com.gaarx.tvplayer.data.EPGRepository
import com.gaarx.tvplayer.domain.model.EPGProgramItem
import com.gaarx.tvplayer.domain.model.toDomain
import javax.inject.Inject

class GetNextProgramUseCase @Inject  constructor(private val epgRepository: EPGRepository) {

    suspend operator fun invoke(channelId : Long): EPGProgramItem? {
        return epgRepository.getNextProgramForChannel(channelId)?.toDomain()
    }
}