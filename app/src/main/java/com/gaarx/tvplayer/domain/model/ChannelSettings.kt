package com.gaarx.tvplayer.domain.model

class ChannelSettings(name: String) {
    private var name: String? = null

    init {
        this.name = name
    }
    fun getName() = name

    companion object {
        const val SOURCES = 0
        const val AUDIO_TRACKS = 1
        const val SUBTITLES_TRACKS = 2
        const val VIDEO_TRACKS = 3
        const val ASPECT_RATIO = 4
        const val UPDATE_EPG = 5
        const val SHOW_EPG = 6
        const val UPDATE_CHANNEL_LIST = 7
        const val CONFIG_URL = 8
    }
}
