package com.gaarx.tvplayer.domain

import com.gaarx.tvplayer.data.EPGRepository
import com.gaarx.tvplayer.domain.model.CategoryItem
import javax.inject.Inject


class UpdateEPGUseCase @Inject constructor(
    private val epgRepository: EPGRepository) {

    suspend operator fun invoke(categoryList: List<CategoryItem>) {
        for (category in categoryList) {
            for (channel in category.channels) {
                channel.epgPrograms = epgRepository.getEPGProgramsForChannel(channel.id)
            }
        }
    }
}