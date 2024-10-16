package com.gaarj.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gaarj.iptvplayer.domain.model.StreamSourceItem

@Entity(
    tableName = "stream_source",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channel_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StreamSourceTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["stream_source_type_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class StreamSourceEntity (

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "index")
    val index: Int,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "refresh_rate")
    val refreshRate: Float,

    @ColumnInfo(name = "channel_id")
    val channelId : Long,

    @ColumnInfo(name = "stream_source_type_id")
    val streamSourceTypeId : Long?,
)

fun StreamSourceItem.toDatabase(channelId: Long) = StreamSourceEntity(
    name = name,
    index = index,
    url = url,
    channelId = channelId,
    refreshRate = refreshRate,
    streamSourceTypeId = 1
)
