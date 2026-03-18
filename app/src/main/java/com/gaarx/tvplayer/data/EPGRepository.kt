package com.gaarx.tvplayer.data

import com.gaarx.tvplayer.data.dao.EPGDao
import com.gaarx.tvplayer.data.database.entities.EPGProgramEntity
import com.gaarx.tvplayer.data.database.entities.toDatabase
import com.gaarx.tvplayer.data.services.EPGService
import com.gaarx.tvplayer.domain.model.EPGProgramItem
import com.gaarx.tvplayer.domain.model.toDomain
import java.net.URI
import javax.inject.Inject


class EPGRepository @Inject constructor(
    private val epgDao: EPGDao,
    private val epgService: EPGService,
    private val settingsRepository: SettingsRepository
){

    private suspend fun insertEPGProgram(epgProgram: EPGProgramEntity) {
        epgDao.insertEPGProgram(epgProgram)
    }

    suspend fun getCurrentProgramForChannel(channelId: Long) : EPGProgramEntity? {
        return epgDao.getCurrentProgramForChannel(channelId, System.currentTimeMillis())
    }

    suspend fun getNextProgramForChannel(channelId: Long) : EPGProgramEntity? {
        return epgDao.getNextProgramForChannel(channelId, System.currentTimeMillis())
    }

    suspend fun getEPGProgramsForChannel(channelId: Long): List<EPGProgramItem> {
        val epgPrograms = epgDao.getEPGProgramsForChannel(channelId)
        return epgPrograms.map { epgProgramEntity ->
            epgProgramEntity.toDomain()
        }
    }

    suspend fun downloadEPG() {
        val now = System.currentTimeMillis()
        val epgSourceUrl = listOf(settingsRepository.getEpgSource())

        var totalInserted = 0
        for (url in epgSourceUrl) {
            val filename = URI(url).path.substringAfterLast("/")
            val success = if (filename.endsWith(".gz")) {
                epgService.downloadAndDecompressGz(filename, url)
            } else if (filename.endsWith(".xml")) {
                epgService.downloadFile(filename, url)
            } else {
                false
            }
            
            if (success) {
                epgService.parseEPGFile(filename) { program ->
                    insertEPGProgram(program.toDatabase().copy(lastUpdated = now))
                    totalInserted++
                }
            }
        }

        // Only cleanup if we actually managed to insert some new data.
        // This prevents wiping the DB if the internet is down during a refresh.
        if (totalInserted > 0) {
            epgDao.deleteOlderThan(now)
        }
    }
}