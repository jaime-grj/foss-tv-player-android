package com.gaarj.iptvplayer.domain.model

data class MediaInfo(
    var videoResolution: String? = null,
    var videoQuality: String? = null,
    var videoCodec: String? = null,
    var videoBitrate: String? = null,
    var videoFrameRate: String? = null,
    var videoAspectRatio: String? = null,
    var audioCodec: String? = null,
    var audioBitrate: String? = null,
    var audioSamplingRate: String? = null,
    var audioChannels: String? = null,
    var hasSubtitles: Boolean = false,
    var hasEPG: Boolean = false,
    var hasMultiLanguageAudio: Boolean = false,
    var hasAudioDescription: Boolean = false,
    var hasTeletext: Boolean = false,
    var hasDRM: Boolean = false
){
}