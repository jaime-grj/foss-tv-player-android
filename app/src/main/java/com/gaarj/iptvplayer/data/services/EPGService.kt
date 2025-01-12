package com.gaarj.iptvplayer.data.services

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Xml
import com.gaarj.iptvplayer.domain.model.EPGProgramItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

class EPGService(private val context: Context) {

    suspend fun downloadAndDecompressGz(filename: String, urlString: String): Boolean {
        val url = URL(urlString)
        return try{
            val urlConnection = withContext(Dispatchers.IO) {
                url.openConnection()
            }
            val gzInputStream = withContext(Dispatchers.IO) {
                GZIPInputStream(urlConnection.getInputStream())
            }
            withContext(Dispatchers.IO) {
                context.openFileOutput(filename, MODE_PRIVATE).use { stream ->
                    stream.write(gzInputStream.readBytes())
                }
            }
            true
        }
        catch (e: IOException) {
            e.printStackTrace()
            false
        }
        catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun downloadFile(filename: String, urlString: String): Boolean {
        val url = URL(urlString)
        return try {

            // Open connection and get InputStream on IO thread
            val urlConnection = withContext(Dispatchers.IO) {
                url.openConnection()
            }
            val inputStream = withContext(Dispatchers.IO) {
                urlConnection.getInputStream()
            }

            // Write the XML data to file on IO thread
            withContext(Dispatchers.IO) {
                context.openFileOutput(filename, MODE_PRIVATE).use { stream ->
                    inputStream.copyTo(stream)  // Copying data directly from input stream to file output stream
                }
            }

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun parseEPGFile(filename: String, onProgramParsed: suspend (EPGProgramItem) -> Unit) {
        val epgDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault())

        val file = File(context.filesDir, filename)
        withContext(Dispatchers.IO) {
            try{
                FileInputStream(file).use { inputStream ->
                    val parser: XmlPullParser = Xml.newPullParser()
                    parser.setInput(inputStream, null)

                    var eventType = parser.eventType
                    var currentTag: String? = null
                    var epgProgramItem: EPGProgramItem? = null

                    var insideRating = false // Track whether we're inside a <rating> tag

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                currentTag = parser.name
                                when (currentTag) {
                                    "programme" -> {
                                        val start = parser.getAttributeValue(null, "start")
                                        val stop = parser.getAttributeValue(null, "stop")
                                        val channel = parser.getAttributeValue(null, "channel")

                                        epgProgramItem = EPGProgramItem(
                                            id = 0,
                                            channelShortname = channel,
                                            title = "",
                                            description = "",
                                            startTime = epgDateFormat.parse(start)!!,
                                            stopTime = epgDateFormat.parse(stop)!!,
                                            category = "",
                                            icon = "",
                                            ageRating = "",
                                            ageRatingIcon = ""
                                        )
                                    }
                                    "rating" -> insideRating = true // Entering a <rating> tag
                                    "icon" -> {
                                        if (insideRating && epgProgramItem != null) {
                                            // Parse as rating icon
                                            val iconSrc = parser.getAttributeValue(null, "src")
                                            if (!iconSrc.isNullOrEmpty()) {
                                                epgProgramItem.ageRatingIcon = iconSrc
                                            }
                                        } else if (!insideRating && epgProgramItem != null) {
                                            // Parse as program icon
                                            val iconSrc = parser.getAttributeValue(null, "src")
                                            if (!iconSrc.isNullOrEmpty()) {
                                                epgProgramItem.icon = iconSrc
                                            }
                                        }
                                    }
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (epgProgramItem != null && currentTag != null) {
                                    val text = parser.text.trim() // Trim whitespace
                                    if (text.isNotEmpty()) {
                                        when (currentTag) {
                                            "title" -> epgProgramItem.title = text
                                            "desc" -> epgProgramItem.description = text
                                            "category" -> epgProgramItem.category = text
                                            "value" -> epgProgramItem.ageRating = text
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                when (parser.name) {
                                    "rating" -> insideRating = false // Exiting a <rating> tag
                                    "programme" -> {
                                        if (epgProgramItem != null) {
                                            // Send the parsed program item back via callback
                                            onProgramParsed(epgProgramItem)
                                            epgProgramItem = null
                                        }
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

}