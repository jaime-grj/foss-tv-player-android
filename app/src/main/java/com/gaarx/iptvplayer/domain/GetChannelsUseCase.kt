package com.gaarx.iptvplayer.domain

import android.util.Log
import com.gaarx.iptvplayer.data.ChannelRepository
import com.gaarx.iptvplayer.domain.model.CategoryItem
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke(): List<CategoryItem> {
        Log.d("GetChannelsUseCase", "Loading channels...")
        val categories = repository.getCategoriesWithChannels()
        Log.d("GetChannelsUseCase", "Loaded ${categories.size} categories")
        return categories
    }
}