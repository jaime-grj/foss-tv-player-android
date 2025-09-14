package com.gaarx.tvplayer.domain

import android.util.Log
import com.gaarx.tvplayer.data.SettingsRepository
import javax.inject.Inject

class UpdateConfigURLUseCase @Inject constructor(private val repository: SettingsRepository) {

    suspend operator fun invoke(newUrl: String) {
        repository.updateConfigURL(newUrl)
        Log.d("UpdateConfigURLUseCase", "Updated config URL to: $newUrl")
    }
}