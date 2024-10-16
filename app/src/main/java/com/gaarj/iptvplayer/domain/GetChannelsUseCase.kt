package com.gaarj.iptvplayer.domain

import com.gaarj.iptvplayer.data.ChannelRepository
import com.gaarj.iptvplayer.data.StreamSourceTypeRepository
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.ChannelProvider
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(private val repository: ChannelRepository, private val rep2: StreamSourceTypeRepository){

    suspend operator fun invoke(): List<ChannelItem> {
        repository.deleteAll()
        if (ChannelProvider.channels.isEmpty()) {
            ChannelProvider.loadChannelList()
        }
        for (channel in ChannelProvider.channels) {
            rep2.insertStreamSourceType(
                StreamSourceTypeItem(
                    id = 1,
                    name = "IPTV",
                    description = null
                ).toDatabase()
            )
            repository.insertChannelWithAllData(channelItem = channel)
        }
        return repository.getFavouriteChannels()
    }

}