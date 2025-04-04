package com.gaarx.iptvplayer.domain.model

import com.gaarx.iptvplayer.data.database.entities.StreamSourceHeaderEntity

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