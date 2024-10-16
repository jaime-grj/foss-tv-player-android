package com.gaarj.iptvplayer.domain.model

data class VideoTrack (
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    var isSelected: Boolean = false
)