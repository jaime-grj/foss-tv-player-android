package com.gaarx.iptvplayer.domain.model

import com.gaarx.iptvplayer.data.database.entities.ChannelShortnameEntity

data class ChannelShortnameItem (
    val id: Long,
    val name: String
)

fun ChannelShortnameEntity.toDomain() = ChannelShortnameItem(
    id = id,
    name = shortName
)