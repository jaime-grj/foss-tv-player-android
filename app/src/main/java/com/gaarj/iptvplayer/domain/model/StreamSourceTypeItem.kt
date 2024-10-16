package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.StreamSourceTypeEntity

data class StreamSourceTypeItem (
    val id: Long?,
    val name: String,
    val description: String?
)

fun StreamSourceTypeEntity.toDomain() = StreamSourceTypeItem(
    id = id,
    name = name,
    description = description
)