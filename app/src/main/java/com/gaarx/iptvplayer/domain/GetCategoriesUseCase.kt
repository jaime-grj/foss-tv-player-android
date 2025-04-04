package com.gaarx.iptvplayer.domain

import android.util.Log
import com.gaarx.iptvplayer.data.ChannelRepository
import com.gaarx.iptvplayer.domain.model.CategoryItem
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke(): List<CategoryItem> {
        Log.d("GetCategoriesUseCase", "Loading channels...")
        return repository.fetchAllCategories()
    }

}