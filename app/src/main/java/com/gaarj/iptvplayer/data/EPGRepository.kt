package com.gaarj.iptvplayer.data

import com.gaarj.iptvplayer.data.dao.ChannelDao
import com.gaarj.iptvplayer.data.dao.EPGDao
import com.gaarj.iptvplayer.data.database.entities.EPGProgramEntity
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.data.services.EPGService
import com.gaarj.iptvplayer.domain.model.EPGProgramItem
import com.gaarj.iptvplayer.domain.model.EPGProvider
import com.gaarj.iptvplayer.domain.model.toDomain
import java.net.URI
import javax.inject.Inject


class EPGRepository @Inject constructor(
    private val epgDao: EPGDao,
    private val epgService: EPGService,
    private val channelDao: ChannelDao
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

        for(url in EPGProvider.epgSourceURLs) {
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