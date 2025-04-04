package com.gaarx.iptvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gaarx.iptvplayer.data.database.entities.ApiResponseKeyEntity

@Dao
interface ApiResponseKeyDao {

    @Insert
    suspend fun insertApiResponseKey(apiResponseKey: ApiResponseKeyEntity) : Long

    @Query("SELECT * FROM api_response_key WHERE api_call_id = :apiCallId")
    suspend fun getApiResponseKeysForApiCall(apiCallId: Long) : List<ApiResponseKeyEntity>
}