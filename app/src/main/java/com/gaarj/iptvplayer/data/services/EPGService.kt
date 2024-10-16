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

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                currentTag = parser.name
                                if (currentTag == "programme") {
                                    val start = parser.getAttributeValue(null, "start")
                                    val stop = parser.getAttributeValue(null, "stop")
                                    val channel = parser.getAttributeValue(null, "channel")

                                    epgProgramItem = EPGProgramItem(
                                        channelShortname = channel,
                                        title = "",
                                        description = "",
                                        startTime = epgDateFormat.parse(start)!!,
                                        stopTime = epgDateFormat.parse(stop)!!,
                                        category = "",
                                        icon = ""
                                    )
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
                                            "icon" -> epgProgramItem.icon = text
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "programme" && epgProgramItem != null) {
                                    // Send the parsed program item back via callback
                                    onProgramParsed(epgProgramItem)
                                    epgProgramItem = null
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