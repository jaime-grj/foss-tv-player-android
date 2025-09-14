package com.gaarx.tvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarx.tvplayer.data.database.entities.StreamSourceHeaderEntity

@Dao
interface StreamSourceHeaderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamSourceHeader(streamSourceHeader: StreamSourceHeaderEntity) : Long

    @Query("SELECT * FROM stream_source_header WHERE stream_source_id = :streamSourceId")
    suspend fun getStreamSourceHeadersForStreamSource(streamSourceId: Long) : List<StreamSourceHeaderEntity>
}