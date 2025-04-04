package com.gaarx.iptvplayer.domain.model

import com.gaarx.iptvplayer.data.database.entities.ApiCallHeaderEntity

data class ApiCallHeaderItem(
    val id: Long,
    val key: String,
    val value: String
)

fun ApiCallHeaderEntity.toDomain() = ApiCallHeaderItem(
    id = id,
    key = key,
    value = value
)