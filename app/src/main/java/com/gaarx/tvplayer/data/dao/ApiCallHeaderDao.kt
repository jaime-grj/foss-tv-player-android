package com.gaarx.tvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarx.tvplayer.data.database.entities.ApiCallHeaderEntity

@Dao
interface ApiCallHeaderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiCallHeader(apiCallHeader: ApiCallHeaderEntity) : Long

    @Query("SELECT * FROM api_call_header WHERE api_call_id = :apiCallId")
    suspend fun getApiCallHeadersForApiCall(apiCallId: Long) : List<ApiCallHeaderEntity>
}