package com.gaarj.iptvplayer.domain

import android.util.Log
import com.gaarj.iptvplayer.data.ChannelRepository
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.ChannelProvider
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
import dagger.hilt.android.scopes.ActivityRetainedScoped
import org.json.JSONObject
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke(categoryId: Long): List<ChannelItem> {
        Log.d("GetChannelsUseCase", "Loading channels...")
        if (categoryId == -1L) {
            val channels = repository.getFavouriteChannels()
            Log.d("GetChannelsUseCase", "Loaded ${channels.size} channels")
            return channels
        }
        else{
            Log.d("GetChannelsUseCase", "Loading channels for category $categoryId")
            val channels = repository.getChannelsByCategory(categoryId)
            Log.d("GetChannelsUseCase", "Loaded ${channels.size} channels")
            return channels
        }
    }

}