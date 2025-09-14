package com.gaarx.tvplayer.domain.model

enum class StreamSourceTypeItem(val value: Int) {
    IPTV(0),
    TUNER(1),
    YOUTUBE(2),
    TWITCH(3);
    companion object {
        private val VALUES = values()
        fun fromInt(value: Int) = VALUES.firstOrNull { it.value == value }
    }
}