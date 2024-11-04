package com.gaarj.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gaarj.iptvplayer.domain.model.ProxyItem

@Entity(
    tableName = "proxy",
    foreignKeys = [
        ForeignKey(
            entity = StreamSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["stream_source_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProxyEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "hostname")
    val hostname: String,

    @ColumnInfo(name = "port")
    val port: Int,

    @ColumnInfo(name = "stream_source_id")
    val streamSourceId: Long
)

fun ProxyItem.toDatabase(streamSourceId: Long) = ProxyEntity(
    id = id,
    hostname = hostname,
    port = port,
    streamSourceId = streamSourceId
)