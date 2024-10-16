package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.StreamSourceHeaderEntity

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