package com.gaarx.iptvplayer.domain

import android.util.Log
import com.gaarx.iptvplayer.data.SettingsRepository
import javax.inject.Inject

class GetConfigURLUseCase @Inject constructor(private val repository: SettingsRepository) {

    suspend operator fun invoke(): String {
        Log.d("GetConfigURLUseCase", "Loading channels...")
        val url = repository.getConfigURL()
        Log.d("GetConfigURLUseCase", "Loaded config URL: $url")
        return url
    }
}