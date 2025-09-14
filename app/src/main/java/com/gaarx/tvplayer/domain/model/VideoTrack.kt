package com.gaarx.tvplayer.domain.model

data class VideoTrack (
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val codec: String,
    var isSelected: Boolean = false
)