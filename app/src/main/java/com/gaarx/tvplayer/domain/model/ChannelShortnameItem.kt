package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.ChannelShortnameEntity

data class ChannelShortnameItem (
    val id: Long,
    val name: String
)

fun ChannelShortnameEntity.toDomain() = ChannelShortnameItem(
    id = id,
    name = shortName
)