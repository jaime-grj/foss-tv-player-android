package com.gaarx.tvplayer.domain

import com.gaarx.tvplayer.data.EPGRepository
import javax.inject.Inject


class DownloadEPGUseCase @Inject constructor(
    private val epgRepository: EPGRepository) {

    suspend operator fun invoke() {
        epgRepository.downloadEPG()
    }
}