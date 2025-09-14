package com.gaarx.tvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarx.tvplayer.data.database.entities.ApiCallEntity

@Dao
interface ApiCallDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiCall(apiCall: ApiCallEntity) : Long

    @Query("SELECT * FROM api_call WHERE stream_source_id = :streamSourceId")
    suspend fun getApiCallsForStreamSource(streamSourceId: Long): List<ApiCallEntity>
}