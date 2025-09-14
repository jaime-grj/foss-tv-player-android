package com.gaarx.tvplayer.domain.model

data class SubtitlesTrack (
    val id: String,
    val language: String,
    val codec: String,
    var isSelected: Boolean = false
)