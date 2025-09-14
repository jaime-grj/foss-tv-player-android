package com.gaarx.tvplayer.core

import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.Tracks
import org.json.JSONObject
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
                codec?.startsWith("avc1") == true -> "H.264/AVC"
                codec?.startsWith("avc3") == true -> "H.264/AVC"
                codec?.startsWith("hvc") == true -> "H.265/HEVC"
                codec?.startsWith("hev") == true -> "H.265/HEVC"
                codec?.startsWith("vp9") == true -> "VP9"
                codec?.startsWith("video/x-vnd.on2.vp9") == true -> "VP9"
                codec?.startsWith("vp08") == true -> "VP8"
                codec?.startsWith("video/av01") == true -> "AV1"
                codec?.startsWith("video/mpeg2") == true -> "MPEG-2 Video"
                // Audio codecs
                codec?.startsWith("mp4a") == true -> "AAC"
                codec?.startsWith("ac-3") == true -> "Dolby Digital (AC-3)"
                codec?.startsWith("audio/eac3") == true -> "Dolby Digital+ (E-AC-3)"
                codec?.startsWith("audio/ac3") == true -> "Dolby Digital (AC-3)"
                codec?.startsWith("audio/ac4") == true -> "Dolby AC-4"
                codec?.startsWith("audio/opus") == true -> "Opus Audio"
                codec?.startsWith("audio/flac") == true -> "FLAC"
                codec?.startsWith("audio/vorbis") == true -> "Vorbis"
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
            return abs(rate1 - rate2) < tolerance
        }

        fun generateDrmBodyFromApiResponse(apiResponse: String): String {
            try{
                val message = JSONObject(apiResponse).getString("Message").trim()
                val keys = message.trim().split("\n").filter { it.isNotEmpty() }
                println("keys:$keys")
                val jsonKeys = keys.map { keyPair ->
                    val (keyId, key) = keyPair.split(":")
                    val keyBytes = key.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    val encodedKey = Base64.encodeToString(keyBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

                    val keyIdBytes = keyId.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    val encodedKeyId = Base64.encodeToString(keyIdBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

                    """{"kty":"oct","k":"$encodedKey","kid":"$encodedKeyId"}"""
                }
                return """{"keys":[${jsonKeys.joinToString(",")}],"type":"temporary"}"""
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }

        fun generateDrmBodyFromKeys(drmKeys: String): String {
            try{
                val keys = drmKeys.trim().split("\n").filter { it.isNotEmpty() }
                println("keys:$keys")
                val jsonKeys = keys.map { keyPair ->
                    val (keyId, key) = keyPair.split(":")
                    val keyBytes = key.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    val encodedKey = Base64.encodeToString(keyBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

                    val keyIdBytes = keyId.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    val encodedKeyId = Base64.encodeToString(keyIdBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

                    """{"kty":"oct","k":"$encodedKey","kid":"$encodedKeyId"}"""
                }
                return """{"keys":[${jsonKeys.joinToString(",")}],"type":"temporary"}"""
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }


        fun getHighestResolution(tracks: Tracks) : Int {
            var lastPixelCount = 0
            var lastHeight = 0
            for (trackGroup in tracks.groups) {
                if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                    for (i in 0 until trackGroup.length) {
                        val trackFormat = trackGroup.getTrackFormat(i)
                        if ((trackFormat.height * trackFormat.width) > lastPixelCount) {
                            lastHeight = trackFormat.height
                            lastPixelCount = trackFormat.height * trackFormat.width
                        }
                    }
                }
            }
            return lastHeight
        }
    }
}