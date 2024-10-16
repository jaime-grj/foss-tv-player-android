package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.ApiCallHeaderEntity

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