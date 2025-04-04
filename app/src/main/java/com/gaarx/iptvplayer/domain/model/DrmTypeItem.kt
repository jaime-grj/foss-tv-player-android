package com.gaarx.iptvplayer.domain.model

enum class DrmTypeItem(val value: Int) {
    NONE(0),
    LICENSE(1),
    CLEARKEY(2);
    companion object {
        private val VALUES = values()
        fun fromInt(value: Int) = VALUES.firstOrNull { it.value == value }
    }
}