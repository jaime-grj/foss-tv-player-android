package com.gaarx.iptvplayer.domain.model

import com.gaarx.iptvplayer.data.database.entities.ApiResponseKeyEntity

class ApiResponseKeyItem (
    val id: Long,
    val jsonPath: String,
    val storeKey: String,
    val index: Int
)

fun ApiResponseKeyEntity.toDomain() = ApiResponseKeyItem(
    id = id,
    jsonPath = jsonPath,
    storeKey = storeKey,
    index = index
)