package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.DrmHeaderEntity

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