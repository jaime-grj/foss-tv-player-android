package com.gaarx.tvplayer.domain

import android.util.Log
import com.gaarx.tvplayer.data.ChannelRepository
import com.gaarx.tvplayer.domain.model.CategoryItem
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke(): List<CategoryItem> {
        Log.d("GetCategoriesUseCase", "Loading channels...")
        return repository.fetchAllCategories()
    }

}