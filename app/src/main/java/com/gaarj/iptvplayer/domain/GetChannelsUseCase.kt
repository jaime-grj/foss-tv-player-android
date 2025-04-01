package com.gaarj.iptvplayer.domain

import android.util.Log
import com.gaarj.iptvplayer.data.ChannelRepository
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.domain.model.CategoryItem
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
import dagger.hilt.android.scopes.ActivityRetainedScoped
import org.json.JSONObject
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke(): List<CategoryItem> {
        Log.d("GetChannelsUseCase", "Loading channels...")
        val categories = repository.getCategoriesWithChannels()
        Log.d("GetChannelsUseCase", "Loaded ${categories.size} categories")
        return categories
    }
}