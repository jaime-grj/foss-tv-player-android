package com.gaarx.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaarx.iptvplayer.domain.model.DrmHeaderItem

@Entity(tableName = "drm_header")
data class DrmHeaderEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "stream_source_id")
    val streamSourceId: Long
)

fun DrmHeaderItem.toDatabase(streamSourceId: Long) = DrmHeaderEntity(
    key = key,
    value = value,
    streamSourceId = streamSourceId
)