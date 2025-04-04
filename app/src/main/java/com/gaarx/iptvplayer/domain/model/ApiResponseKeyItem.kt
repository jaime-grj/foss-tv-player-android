package com.gaarx.iptvplayer.domain.model

import com.gaarx.iptvplayer.data.database.entities.ApiResponseKeyEntity

class ApiResponseKeyItem (
    val id: Long,
    val jsonPath: String,
    val index: Int
)

fun ApiResponseKeyEntity.toDomain() = ApiResponseKeyItem(
    id = id,
    jsonPath = jsonPath,
    index = index
)