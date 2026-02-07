package com.gaarx.tvplayer.domain

import android.util.Log
import com.gaarx.tvplayer.data.ChannelRepository
import javax.inject.Inject

class ImportJSONDataUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke(): Boolean {
        Log.d("ImportJSONDataUseCase", "Importing JSON data")
        val result = repository.reloadChannelsFromJSON()
        Log.d("ImportJSONDataUseCase", "Imported JSON data")
        return result
    }

}