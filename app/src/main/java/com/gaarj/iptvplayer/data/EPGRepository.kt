package com.gaarj.iptvplayer.data

import com.gaarj.iptvplayer.data.dao.ChannelDao
import com.gaarj.iptvplayer.data.dao.EPGDao
import com.gaarj.iptvplayer.data.database.entities.EPGProgramEntity
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.data.services.EPGService
import com.gaarj.iptvplayer.domain.model.EPGProvider
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

    // Process each EPG item as it's loaded and write to the DB
    suspend fun downloadEPG() {
        epgDao.deleteAll()  // Clear old data first

        for(url in EPGProvider.epgSourceURLs) {
            val uri = URI(url)
            val path = uri.path
            val filename = path.substring(path.lastIndexOf('/') + 1)
            epgService.downloadAndDecompressGz(filename, url)
            epgService.parseEPGFile(filename) { insertEPGProgram(it.toDatabase()) }
        }

    // Process and insert each program into the DB
        /*EPGReader.loadEPG { program ->
            insertEPGProgram(program.toDatabase())
        }*/
    }

}