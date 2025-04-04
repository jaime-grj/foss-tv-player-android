package com.gaarx.iptvplayer.domain

import android.util.Log
import com.gaarx.iptvplayer.data.ChannelRepository
import javax.inject.Inject

class ImportJSONDataUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke() {
        repository.deleteAll()
        Log.d("ImportJSONDataUseCase", "Importing JSON data")
        repository.loadChannelsFromJSON()
        Log.d("ImportJSONDataUseCase", "Imported JSON data")
    }

}