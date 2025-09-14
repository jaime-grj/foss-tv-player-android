package com.gaarx.tvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaarx.tvplayer.domain.model.EPGProgramItem
import java.util.Date

@Entity(
    tableName = "epg_program")
data class EPGProgramEntity (

    @PrimaryKey (autoGenerate = true)
    @ColumnInfo(name = "id")
    val id : Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "start_time")
    val startTime: Date,

    @ColumnInfo(name = "stop_time")
    val stopTime: Date,

    @ColumnInfo(name = "channel_shortname")
    val channelShortname: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "ageRating")
    val ageRating: String?,

    @ColumnInfo(name = "ageRatingIcon")
    val ageRatingIcon: String?,

    @ColumnInfo(name = "lastUpdated")
    val lastUpdated: Long
)

fun EPGProgramItem.toDatabase() : EPGProgramEntity =
    EPGProgramEntity(
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