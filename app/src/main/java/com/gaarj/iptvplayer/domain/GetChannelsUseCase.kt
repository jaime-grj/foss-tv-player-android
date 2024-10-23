package com.gaarj.iptvplayer.domain

import com.gaarj.iptvplayer.data.ChannelRepository
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.ChannelProvider
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
import org.json.JSONObject
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke(): List<ChannelItem> {
        repository.deleteAll()
        /*if (ChannelProvider.channels.isEmpty()) {
            ChannelProvider.loadChannelList()
        }
        rep2.insertStreamSourceType(
            StreamSourceTypeItem(
                id = 1,
                name = "IPTV",
                description = null
            ).toDatabase()
        )
        for (channel in ChannelProvider.channels) {
            repository.insertChannelWithAllData(channelItem = channel)
        }*/

        repository.loadChannelsFromJSON()
        return repository.getFavouriteChannels()
    }

}