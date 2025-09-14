package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.StreamSourceHeaderEntity

class StreamSourceHeaderItem (
    val id: Long,
    val key: String,
    val value: String
)

fun StreamSourceHeaderEntity.toDomain() = StreamSourceHeaderItem(
    id = id,
    key = key,
    value = value
)