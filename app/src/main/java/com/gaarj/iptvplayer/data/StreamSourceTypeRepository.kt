package com.gaarj.iptvplayer.data

import androidx.room.Transaction
import javax.inject.Inject
import com.gaarj.iptvplayer.data.dao.StreamSourceTypeDao
import com.gaarj.iptvplayer.data.database.entities.StreamSourceTypeEntity

class StreamSourceTypeRepository @Inject constructor(
    private val streamSourceTypeDao: StreamSourceTypeDao
) {

    @Transaction
    suspend fun insertStreamSourceType(streamSourceType: StreamSourceTypeEntity) =
        streamSourceTypeDao.insertStreamSourceType(streamSourceType)
}