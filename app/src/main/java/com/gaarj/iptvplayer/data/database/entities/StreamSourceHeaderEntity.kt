package com.gaarj.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaarj.iptvplayer.domain.model.StreamSourceHeaderItem

@Entity(
    tableName = "stream_source_header",
)
class StreamSourceHeaderEntity (

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0,

    @ColumnInfo(name = "key")
    var key: String,

    @ColumnInfo(name = "value")
    var value: String,

    @ColumnInfo(name = "stream_source_id")
    var streamSourceId: Long
)

fun StreamSourceHeaderItem.toDatabase(streamSourceId: Long) = StreamSourceHeaderEntity(
    key = key,
    value = value,
    streamSourceId = streamSourceId
)