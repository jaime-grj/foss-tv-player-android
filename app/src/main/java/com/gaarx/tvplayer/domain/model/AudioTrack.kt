package com.gaarx.tvplayer.domain.model

data class AudioTrack(
    val id: String,
    val language: String,
    val codec: String,
    val bitrate: Int,
    val channelCount: Int,
    var isSelected: Boolean = false
)