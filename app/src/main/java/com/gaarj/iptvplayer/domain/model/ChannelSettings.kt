package com.gaarj.iptvplayer.domain.model

class ChannelSettings(name: String) {
    private var name: String? = null

    init {
        this.name = name
    }
    fun getName() = name

    companion object {
        const val AUDIO_TRACKS = 0
        const val SUBTITLES_TRACKS = 1
        const val VIDEO_TRACKS = 2
        const val SOURCES = 3
    }
}
