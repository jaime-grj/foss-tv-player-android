package com.gaarx.tvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarx.tvplayer.data.database.entities.ApiCallEntity
import com.gaarx.tvplayer.data.database.entities.StreamSourceEntity

@Dao
interface StreamSourceDao {

    // This is an optional helper method to insert a list of channels
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamSource(streamSource: StreamSourceEntity) : Long

    @Query("SELECT * FROM stream_source WHERE channel_id = :channelId")
    suspend fun getStreamSourcesForChannel(channelId: Long): List<StreamSourceEntity>

    @Query("SELECT * FROM api_call WHERE stream_source_id = :streamSourceId")
    suspend fun getApiCallsForStreamSource(streamSourceId: Long): List<ApiCallEntity>


}
