package com.gaarx.iptvplayer.data

import com.gaarx.iptvplayer.data.dao.EPGDao
import com.gaarx.iptvplayer.data.database.entities.EPGProgramEntity
import com.gaarx.iptvplayer.data.database.entities.toDatabase
import com.gaarx.iptvplayer.data.services.EPGService
import com.gaarx.iptvplayer.domain.model.EPGProgramItem
import com.gaarx.iptvplayer.domain.model.toDomain
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
        return epgDao.getCurrentProgramForChannel(channelId)
    }

    suspend fun getNextProgramForChannel(channelId: Long) : EPGProgramEntity? {
        return epgDao.getNextProgramForChannel(channelId)
    }

    suspend fun getEPGProgramsForChannel(channelId: Long): List<EPGProgramItem> {
        val epgPrograms = epgDao.getEPGProgramsForChannel(channelId)
        return epgPrograms.map { epgProgramEntity ->
            epgProgramEntity.toDomain()
        }
    }

    // Process each EPG item as it's loaded and write to the DB
    suspend fun downloadEPG() {
        epgDao.deleteAll()  // Clear old data first

        val epgSourceUrl = listOf(settingsRepository.getEpgSource()) // Will add multiple sources later
        for(url in epgSourceUrl) {
            val uri = URI(url)
            val path = uri.path
            val filename = path.substring(path.lastIndexOf('/') + 1)
            if (filename.endsWith(".gz")) {
                epgService.downloadAndDecompressGz(filename, url)
            }
            else if (filename.endsWith(".xml")) {
                epgService.downloadFile(filename, url)
            }
            epgService.parseEPGFile(filename) { insertEPGProgram(it.toDatabase()) }
        }
    }

}