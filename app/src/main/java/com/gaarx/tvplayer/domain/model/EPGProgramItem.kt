package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.EPGProgramEntity
import java.util.Date

data class EPGProgramItem (
    var id: Long,
    var title: String,
    var description: String,
    val startTime: Date,
    val stopTime: Date,
    val channelShortname: String,
    var category: String,
    var icon: String,
    var ageRating: String? = null,
    var ageRatingIcon: String? = null,
    var lastUpdated: Long = System.currentTimeMillis()
)

fun EPGProgramEntity.toDomain() = EPGProgramItem(
    id = id,
    title = title,
    description = description,
    startTime = startTime,
    stopTime = stopTime,
    channelShortname = channelShortname,
    category = category,
    icon = icon,
    ageRating = ageRating,
    ageRatingIcon = ageRatingIcon,
    lastUpdated = lastUpdated
)