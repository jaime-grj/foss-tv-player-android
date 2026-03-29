package com.gaarx.tvplayer.data.services

import android.content.Context
import android.util.Log
import android.util.Xml
import com.gaarx.tvplayer.domain.model.EPGProgramItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

/**
 * Service responsible for downloading and parsing EPG (Electronic Program Guide) files.
 */
class EPGService(private val context: Context) {

    companion object {
        private const val TAG = "EPGService"
        private const val EPG_DATE_FORMAT = "yyyyMMddHHmmss Z"
    }

    /**
     * Downloads a gzipped EPG file, decompresses it, and saves it to internal storage.
     */
    suspend fun downloadAndDecompressGz(filename: String, urlString: String): Boolean {
        return downloadAndSaveFile(filename, urlString, isGzip = true)
    }

    /**
     * Downloads a plain EPG file and saves it to internal storage.
     */
    suspend fun downloadFile(filename: String, urlString: String): Boolean {
        return downloadAndSaveFile(filename, urlString, isGzip = false)
    }

    private suspend fun downloadAndSaveFile(filename: String, urlString: String, isGzip: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection()
                val inputStream = connection.getInputStream().let {
                    if (isGzip) GZIPInputStream(it) else it
                }

                inputStream.use { input ->
                    context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download EPG file from $urlString", e)
                false
            }
        }

    /**
     * Parses the EPG file and executes [onProgramParsed] for each program found.
     */
    suspend fun parseEPGFile(filename: String, onProgramParsed: suspend (EPGProgramItem) -> Unit) {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) {
                Log.w(TAG, "EPG file not found: $filename")
                return@withContext
            }

            val dateFormat = SimpleDateFormat(EPG_DATE_FORMAT, Locale.getDefault())

            try {
                FileInputStream(file).use { fis ->
                    val parser = Xml.newPullParser().apply {
                        setInput(fis, null)
                    }

                    var eventType = parser.eventType
                    var currentTag: String? = null
                    var epgProgramItem: EPGProgramItem? = null
                    var insideRating = false

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                currentTag = parser.name
                                when (currentTag) {
                                    "programme" -> {
                                        val start = parser.getAttributeValue(null, "start")
                                        val stop = parser.getAttributeValue(null, "stop")
                                        val channel = parser.getAttributeValue(null, "channel")

                                        if (start != null && stop != null) {
                                            try {
                                                epgProgramItem = EPGProgramItem(
                                                    id = 0,
                                                    channelShortname = channel ?: "",
                                                    title = "",
                                                    description = "",
                                                    startTime = dateFormat.parse(start)!!,
                                                    stopTime = dateFormat.parse(stop)!!,
                                                    category = "",
                                                    icon = "",
                                                    ageRating = "",
                                                    ageRatingIcon = ""
                                                )
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error parsing dates for programme: $start, $stop", e)
                                            }
                                        }
                                    }
                                    "rating" -> insideRating = true
                                    "icon" -> {
                                        val iconSrc = parser.getAttributeValue(null, "src")
                                        if (!iconSrc.isNullOrEmpty() && epgProgramItem != null) {
                                            if (insideRating) epgProgramItem.ageRatingIcon = iconSrc
                                            else epgProgramItem.icon = iconSrc
                                        }
                                    }
                                }
                            }
                            XmlPullParser.TEXT -> {
                                val text = parser.text?.trim() ?: ""
                                if (text.isNotEmpty() && epgProgramItem != null) {
                                    when (currentTag) {
                                        "title" -> epgProgramItem.title = text
                                        "desc" -> epgProgramItem.description = text
                                        "category" -> epgProgramItem.category = text
                                        "value" -> if (insideRating) epgProgramItem.ageRating = text
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                when (parser.name) {
                                    "rating" -> insideRating = false
                                    "programme" -> {
                                        epgProgramItem?.let {
                                            onProgramParsed(it)
                                            epgProgramItem = null
                                        }
                                    }
                                }
                                currentTag = null
                            }
                        }
                        eventType = parser.next()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing EPG file: $filename", e)
            }
        }
    }
}
