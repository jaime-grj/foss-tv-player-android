package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.ApiResponseKeyEntity

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