package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.ApiCallHeaderEntity

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