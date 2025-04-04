package com.gaarx.iptvplayer.domain

import com.gaarx.iptvplayer.data.EPGRepository
import com.gaarx.iptvplayer.data.SettingsRepository
import com.gaarx.iptvplayer.domain.model.CategoryItem
import javax.inject.Inject


class UpdateEPGUseCase @Inject constructor(
    private val epgRepository: EPGRepository,
    private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(categoryList: List<CategoryItem>) {
        for (category in categoryList) {
            for (channel in category.channels) {
                channel.epgPrograms = epgRepository.getEPGProgramsForChannel(channel.id)
            }
        }
    }
}