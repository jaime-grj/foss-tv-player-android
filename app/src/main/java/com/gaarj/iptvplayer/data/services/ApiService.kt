package com.gaarj.iptvplayer.data.services

import android.util.Log
import com.gaarj.iptvplayer.domain.model.ApiCallHeaderItem
import com.gaarj.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarj.iptvplayer.domain.model.StreamSourceItem
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class ApiService {

    companion object {

        private const val CONNECT_TIMEOUT = 6000

        private fun getJSONFromURL(url: String, method: String? = "GET", headers: Map<String, String>? = null, body: String? = null): String {
            var json = ""
            var connection: HttpURLConnection? = null
            try {
                val urlObj = URL(url)
                connection = urlObj.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.addRequestProperty("Host", urlObj.host)
                if (method == "POST") {
                    connection.addRequestProperty("Content-Length", body?.length.toString())
                }
                if (headers != null) {
                    for ((key, value) in headers) {
                        connection.addRequestProperty(key, value)
                    }
                }
                if (body != null) {
                    connection.doOutput = true
                    connection.doInput = true
                    val outputStream: OutputStream = connection.outputStream
                    outputStream.write(body.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                    outputStream.close()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    json = response.toString()
                } else {
                    Log.e("ApiService", "Request failed: $responseCode")
                }
            }catch (e: Exception) {
                e.printStackTrace()
                return ""
            } finally {
                connection?.disconnect()
            }
            return json
        }

        private fun getURLFromHTML(url: String, stringSearch: String): String {
            val doc: Document = Jsoup.connect(url).get()
            val elementsWithLinks: List<Element> = doc.select("[href], [src]")
            val matchingLinks = elementsWithLinks.filter { element ->
                val link = element.attr("href").ifEmpty { element.attr("src") }
                link.contains(stringSearch)
            }
            if (matchingLinks.isEmpty()) {
                return ""
            } else {
                matchingLinks.forEach { element ->
                    val link = element.attr("href").ifEmpty { element.attr("src") }
                    return link
                }
            }
            return ""
        }

        fun getURLFromChannelSource(streamSource: StreamSourceItem): String? {
            val apiCalls = streamSource.apiCalls
            var url: String? = null
            var data: String? = null

            if (apiCalls != null) {
                for (apiCall in apiCalls) {
                    url = apiCall.url
                    val index = apiCall.index
                    if (data != null){
                        url = url.replace("{${index - 1}}", data)
                    }
                    val headers = apiCall.headers
                    val headersMap = headers?.let { getHeadersMapFromApiCallHeadersObject(it) }

                    val method = apiCall.method
                    val body = apiCall.body

                    val apiResponseKeys = apiCall.apiResponseKeys

                    if (apiCall.type == "JSON"){
                        val json = getJSONFromURL(url, method, headersMap, body)

                        var i = 0
                        for (apiResponseKey in apiResponseKeys) {
                            val jsonPath = apiResponseKey.jsonPath
                            data = JsonPath.parse(json)?.read<String>(jsonPath)

                            i++
                        }
                    } else if (apiCall.type == "HTML") {
                        url = getURLFromHTML(url, apiCall.stringSearch!!)
                        return url
                    }
                }
            }
            if (url != null) {
                Log.d("ApiService", "data:$data")
                Log.d("ApiService", "url: $url")
                if (data != null){
                    url = streamSource.url.replace("{token}", data)
                }
                else{
                    url = ""
                }
                Log.d("ApiService", "url despues de reemplazar: $url")
            }
            return url
        }

        fun getHeadersMapFromHeadersObject(headers: List<StreamSourceHeaderItem>) : Map<String, String> {
            val headersMap = mutableMapOf<String, String>()
            for (header in headers) {
                headersMap[header.key] = header.value
            }
            return headersMap
        }

        fun getHeadersMapFromApiCallHeadersObject(headers: List<ApiCallHeaderItem>) : Map<String, String> {
            val headersMap = mutableMapOf<String, String>()
            for (header in headers) {
                headersMap[header.key] = header.value
            }
            return headersMap
        }
    }

}