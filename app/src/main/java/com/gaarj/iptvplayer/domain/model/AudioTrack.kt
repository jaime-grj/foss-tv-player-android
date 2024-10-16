package com.gaarj.iptvplayer.domain.model

data class AudioTrack(
    val id: String,
    val language: String,
    var isSelected: Boolean = false
)