package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.EPGProgramEntity
import java.util.Date

data class EPGProgramItem (
    var id: Long,
    var title: String,
    var description: String,
    val startTime: Date,
    val stopTime: Date,
    val channelShortname: String,
    var category: String,
    var icon: String
)

fun EPGProgramEntity.toDomain() = EPGProgramItem(
    id = id,
    title = title,
    description = description,
    startTime = startTime,
    stopTime = stopTime,
    channelShortname = channelShortname,
    category = category,
    icon = icon
)