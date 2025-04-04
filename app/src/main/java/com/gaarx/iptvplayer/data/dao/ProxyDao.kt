package com.gaarx.iptvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarx.iptvplayer.data.database.entities.ProxyEntity

@Dao
interface ProxyDao {

    @Query("SELECT * FROM proxy WHERE stream_source_id = :streamSourceId")
    suspend fun getProxiesForStreamSource(streamSourceId: Long): List<ProxyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxy(proxy: ProxyEntity) : Long

}