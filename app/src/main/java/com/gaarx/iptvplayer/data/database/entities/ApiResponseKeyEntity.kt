package com.gaarx.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaarx.iptvplayer.domain.model.ApiResponseKeyItem

@Entity(
    tableName = "api_response_key",
)
class ApiResponseKeyEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "json_path")
    val jsonPath: String,

    @ColumnInfo(name = "index")
    val index: Int,

    @ColumnInfo(name = "storeKey")
    val storeKey: String,

    @ColumnInfo(name = "api_call_id")
    val apiCallId: Long
)

fun ApiResponseKeyItem.toDatabase(apiCallId: Long) = ApiResponseKeyEntity(
    jsonPath = jsonPath,
    index = index,
    apiCallId = apiCallId,
    storeKey = storeKey
)