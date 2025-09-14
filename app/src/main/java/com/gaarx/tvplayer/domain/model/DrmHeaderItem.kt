package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.DrmHeaderEntity

class DrmHeaderItem (
    val id: Long,
    val key: String,
    val value: String
)

fun DrmHeaderEntity.toDomain() = DrmHeaderItem(
    id = id,
    key = key,
    value = value
)