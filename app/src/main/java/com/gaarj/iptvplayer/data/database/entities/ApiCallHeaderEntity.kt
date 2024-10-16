package com.gaarj.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaarj.iptvplayer.domain.model.ApiCallHeaderItem

@Entity(tableName = "api_call_header")
data class ApiCallHeaderEntity (
    @PrimaryKey (autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "api_call_id")
    val apiCallId: Long
)

fun ApiCallHeaderItem.toDatabase(apiCallId: Long) = ApiCallHeaderEntity(
    key = key,
    value = value,
    apiCallId = apiCallId
)