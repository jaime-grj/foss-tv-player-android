package com.gaarx.iptvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarx.iptvplayer.data.database.entities.EPGProgramEntity

@Dao
interface EPGDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEPGProgram(epgProgram: EPGProgramEntity) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEPGPrograms(epgPrograms: List<EPGProgramEntity>)

    @Query("SELECT * FROM epg_program WHERE channel_shortname IN (SELECT shortname FROM channel_shortname WHERE channel_id = :channelId) AND strftime('%s', 'now') * 1000 BETWEEN start_time AND stop_time")
    suspend fun getCurrentProgramForChannel(channelId: Long) : EPGProgramEntity?

    @Query("SELECT * FROM epg_program WHERE channel_shortname IN (SELECT shortname FROM channel_shortname WHERE channel_id = :channelId) AND start_time > strftime('%s', 'now') * 1000 ORDER BY start_time ASC LIMIT 1")
    suspend fun getNextProgramForChannel(channelId: Long) : EPGProgramEntity?

    @Query("DELETE FROM epg_program")
    suspend fun deleteAll()

    @Query("SELECT * FROM epg_program WHERE channel_shortname IN (SELECT shortname FROM channel_shortname WHERE channel_id = :channelId)")
    suspend fun getEPGProgramsForChannel(channelId: Long): List<EPGProgramEntity>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EPGProgramEntity>)

    @Query("DELETE FROM epg_program WHERE lastUpdated < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}