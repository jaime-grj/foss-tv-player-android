package com.gaarj.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gaarj.iptvplayer.domain.model.ApiCallItem

@Entity(
    tableName = "api_call",
    foreignKeys = [
        ForeignKey(
            entity = StreamSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["stream_source_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class ApiCallEntity (
    @PrimaryKey (autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "index")
    val index: Int,

    @ColumnInfo(name = "method")
    val method: String?,

    @ColumnInfo(name = "body")
    val body: String?,

    @ColumnInfo(name = "string_search")
    val stringSearch : String?,

    @ColumnInfo(name = "type")
    val type: String?,

    @ColumnInfo(name = "stream_source_id")
    val streamSourceId: Long
)

fun ApiCallItem.toDatabase(streamSourceId: Long) = ApiCallEntity(
    url = url,
    index = index,
    method = method,
    body = body,
    stringSearch = stringSearch,
    type = type,
    streamSourceId = streamSourceId
)