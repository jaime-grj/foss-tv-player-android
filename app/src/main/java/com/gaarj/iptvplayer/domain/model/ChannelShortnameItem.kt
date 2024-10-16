package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.ChannelShortnameEntity

data class ChannelShortnameItem (
    val id: Long,
    val name: String
)

fun ChannelShortnameEntity.toDomain() = ChannelShortnameItem(
    id = id,
    name = shortName
)