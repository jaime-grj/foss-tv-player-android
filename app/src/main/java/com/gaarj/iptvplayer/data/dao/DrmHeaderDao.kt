package com.gaarj.iptvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarj.iptvplayer.data.database.entities.DrmHeaderEntity
import com.gaarj.iptvplayer.data.database.entities.StreamSourceHeaderEntity

@Dao
interface DrmHeaderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrmHeader(drmHeader: DrmHeaderEntity) : Long

    @Query("SELECT * FROM drm_header WHERE stream_source_id = :streamSourceId")
    suspend fun getDrmHeadersForStreamSource(streamSourceId: Long) : List<DrmHeaderEntity>
}