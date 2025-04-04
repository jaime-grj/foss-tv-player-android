package com.gaarx.iptvplayer.domain.model

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
        const val UPDATE_EPG = 4
        const val SHOW_EPG = 5
        const val UPDATE_CHANNEL_LIST = 6
    }
}
