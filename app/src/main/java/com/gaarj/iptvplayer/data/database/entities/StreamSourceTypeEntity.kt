package com.gaarj.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem


@Entity(tableName = "stream_source_type")
class StreamSourceTypeEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String?
)

fun StreamSourceTypeItem.toDatabase() = StreamSourceTypeEntity(
    name = name,
    description = description
)