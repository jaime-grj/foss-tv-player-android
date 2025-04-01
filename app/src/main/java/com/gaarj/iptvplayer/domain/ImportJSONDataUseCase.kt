package com.gaarj.iptvplayer.domain

import android.util.Log
import com.gaarj.iptvplayer.data.ChannelRepository
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
import org.json.JSONObject
import javax.inject.Inject

class ImportJSONDataUseCase @Inject constructor(private val repository: ChannelRepository){

    suspend operator fun invoke() {
        repository.deleteAll()
        Log.d("ImportJSONDataUseCase", "Importing JSON data")
        repository.loadChannelsFromJSON()
        Log.d("ImportJSONDataUseCase", "Imported JSON data")
    }

}