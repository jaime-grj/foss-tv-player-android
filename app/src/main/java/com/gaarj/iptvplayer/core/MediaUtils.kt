package com.gaarj.iptvplayer.core

import java.util.Locale
import kotlin.math.abs

class MediaUtils {
    companion object{

        fun calculateVideoQuality(width: Int, height: Int): String? {
            if (width >= height) {
                return when (height) {
                    in 3240..4320 -> "FUHD"
                    in 1920..3239 -> "UHD"
                    in 1280..1919 -> "QHD"
                    in 960..1279 -> "FHD"
                    in 600..959 -> "HD"
                    in 360..599 -> "SD"
                    in 1..359 -> "LD"
                    else -> null
                }
            } else {
                return when (width) {
                    in 2560..4320 -> "FUHD"
                    in 1920..2559 -> "UHD"
                    in 1280..1919 -> "QHD"
                    in 960..1279 -> "FHD"
                    in 600..959 -> "HD"
                    in 360..599 -> "SD"
                    in 1..359 -> "LD"
                    else -> null
                }
            }
        }

        fun getUserFriendlyCodec(codec: String?): String? {
            return when {
                // Video codecs
                codec?.startsWith("avc1") == true -> "H.264"
                codec?.startsWith("avc3") == true -> "H.264"
                codec?.startsWith("hvc") == true -> "H.265"
                codec?.startsWith("hev") == true -> "H.265"
                codec?.startsWith("vp9") == true -> "VP9"
                codec?.startsWith("vp08") == true -> "VP8"
                codec?.startsWith("av1") == true -> "AV1"
                codec?.startsWith("video/mpeg2") == true -> "MPEG-2 Video"
                // Audio codecs
                codec?.startsWith("mp4a") == true -> "AAC"
                codec?.startsWith("ac-3") == true -> "Dolby Digital (AC-3)"
                codec?.startsWith("audio/eac3") == true -> "Dolby Digital+ (E-AC-3)"
                codec?.startsWith("audio/ac3") == true -> "Dolby Digital (AC-3)"
                codec?.startsWith("opus") == true -> "Opus"
                codec?.startsWith("flac") == true -> "FLAC"
                codec?.startsWith("vorbis") == true -> "Vorbis"
                codec?.startsWith("mp3") == true -> "MP3"
                codec?.startsWith("ac-4") == true -> "AC-4"
                codec?.startsWith("audio/mpeg-L2") == true -> "MPEG-2 Audio"
                codec?.startsWith("audio/mpeg2") == true -> "MPEG-2 Audio"
                codec?.startsWith("audio/mpeg") == true -> "MP3"
                // Default case
                else -> codec
            }
        }

        fun getHumanReadableAspectRatio(dar: Float): String {
            val aspectRatios = mapOf(
                "16:9" to 1.78f,
                "4:3" to 1.33f,
                "21:9" to 2.33f,
                "1:1" to 1.0f
            )
            val tolerance = 0.05f // Allow a small margin of error
            // Loop through common aspect ratios and check if DAR is close to any
            for ((label, ratio) in aspectRatios) {
                if (abs(dar - ratio) < tolerance) {
                    return label
                }
            }
            // If no match, return the raw aspect ratio formatted to two decimal places
            return String.format(Locale.getDefault(), "%.2f:1", dar)
        }

        fun areRatesEqual(rate1: Float, rate2: Float, tolerance: Float = 0.5f): Boolean {
            return kotlin.math.abs(rate1 - rate2) < tolerance
        }
    }
}