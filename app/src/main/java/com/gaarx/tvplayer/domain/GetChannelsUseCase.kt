package com.gaarx.tvplayer.domain

import android.util.Log
import com.gaarx.tvplayer.data.ChannelRepository
import com.gaarx.tvplayer.domain.model.CategoryItem
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke(): List<CategoryItem> {
        Log.d("GetChannelsUseCase", "Loading channels...")
        val categories = repository.getCategoriesWithChannels()
        Log.d("GetChannelsUseCase", "Loaded ${categories.size} categories")
        return categories
    }
}