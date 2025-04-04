package com.gaarx.iptvplayer.domain.model

import com.gaarx.iptvplayer.data.database.entities.ChannelEntity

data class ChannelItem(
    val id: Long,
    val name: String,
    val description: String?,
    val logo: String?,
    val language: String?,
    val country: String?,
    val region: String?,
    val subregion: String?,
    val indexFavourite: Int?,
    val indexGroup: Int?,
    val streamSources: List<StreamSourceItem>,
    val relatedChannels: List<ChannelItem>,
    val channelShortnames: List<ChannelShortnameItem>,
    var currentProgram: EPGProgramItem? = null,
    var nextProgram: EPGProgramItem? = null,
    var epgPrograms: List<EPGProgramItem> = listOf()
)

fun ChannelEntity.toDomain(
    streamSources: List<StreamSourceItem> = listOf(),
    relatedChannels: List<ChannelItem> = listOf(),
    channelShortnames: List<ChannelShortnameItem> = listOf(),
    epgPrograms: List<EPGProgramItem> = listOf()
) = ChannelItem(
    id = id,
    name = name,
    description = description,
    logo = logo,
    language = language,
    country = country,
    region = region,
    subregion = subregion,
    indexFavourite = indexFavourite,
    indexGroup = indexGroup,
    streamSources = streamSources,
    relatedChannels = relatedChannels,
    channelShortnames = channelShortnames,
    epgPrograms = epgPrograms
)
