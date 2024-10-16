package com.gaarj.iptvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.gaarj.iptvplayer.data.database.entities.StreamSourceTypeEntity

@Dao
interface StreamSourceTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamSourceType(streamSourceType: StreamSourceTypeEntity) : Long
}