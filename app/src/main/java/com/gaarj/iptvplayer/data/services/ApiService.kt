package com.gaarj.iptvplayer.data.services

import android.util.Log
import com.gaarj.iptvplayer.domain.model.ApiCallHeaderItem
import com.gaarj.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarj.iptvplayer.domain.model.StreamSourceItem
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
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
import java.util.Scanner

class ApiService {

    companion object {

        private const val CONNECT_TIMEOUT = 5000

        private fun getJSONFromURL(url: String, method: String? = "GET", headers: Map<String, String>? = null, body: String? = null): String {
            var json = ""
            var connection: HttpURLConnection? = null
            try {
                Log.d("ApiService", "URL: $url")
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
            try{
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        fun getURLFromChannelSource(streamSource: StreamSourceItem): String? {
            var url: String? = null
            if (streamSource.streamSourceType == StreamSourceTypeItem.IPTV) {
                if (streamSource.apiCalls.isNullOrEmpty()) {
                    url = streamSource.url
                    return url
                }
                val apiCalls = streamSource.apiCalls.sortedBy { it.index }
                var data: String? = null

                for (apiCall in apiCalls) {
                    url = apiCall.url
                    Log.d("ApiService", "URL: $url")
                    val index = apiCall.index
                    if (data != null){
                        url = url.replace("{${index - 1}}", data)
                    }
                    val headers = apiCall.headers
                    val headersMap = headers?.let { getHeadersMapFromApiCallHeadersObject(it) }

                    val method = apiCall.method
                    val body = apiCall.body

                    val apiResponseKeys = apiCall.apiResponseKeys

                    if (apiCall.type == "json"){
                        val json = getJSONFromURL(url, method, headersMap, body)

                        for (apiResponseKey in apiResponseKeys) {
                            val jsonPath = apiResponseKey.jsonPath
                            data = JsonPath.parse(json)?.read<String>(jsonPath)
                        }
                    } else if (apiCall.type == "html") {
                        url = getURLFromHTML(url, apiCall.stringSearch!!)
                        return url
                    }
                }
                if (url != null) {
                    Log.d("ApiService", "data:$data")
                    Log.d("ApiService", "url: $url")
                    url = if (data != null){
                        streamSource.url.replace("{token}", data)
                    } else{
                        ""
                    }
                    Log.d("ApiService", "url despues de reemplazar: $url")
                }

            }
            else if (streamSource.streamSourceType == StreamSourceTypeItem.TWITCH) {
                val clientId = "ue6666qo983tsx6so1t0vnawi233wa"
                val gqlUrl = "https://gql.twitch.tv/gql#origin=twilight"
                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36"
                val headers = mapOf(
                    "Client-Id" to clientId,
                    "User-Agent" to userAgent)
                val gql = "[\n" +
                        "   {\n" +
                        "      \"operationName\":\"StreamMetadata\",\n" +
                        "      \"query\":\"query StreamMetadata(\$channelLogin: String!) { user(login: \$channelLogin) { id primaryColorHex isPartner profileImageURL(width: 70) primaryTeam { id name displayName } squadStream { id members { id } status } channel { id chanlets { id } } lastBroadcast { id title } stream { id type createdAt game { id name } } } }\",\n" +
                        "      \"variables\":{\n" +
                        "         \"channelLogin\":\"" + streamSource.url + "\"\n" +
                        "      },\n" +
                        "      \"extensions\":{\n" +
                        "         \"persistedQuery\":{\n" +
                        "            \"version\":1,\n" +
                        "            \"sha256Hash\":\"a647c2a13599e5991e175155f798ca7f1ecddde73f7f341f39009c14dbf59962\"\n" +
                        "         }\n" +
                        "      }\n" +
                        "   },\n" +
                        "   {\n" +
                        "      \"query\":\"query UseViewCount(\$channelLogin: String!) { user(login: \$channelLogin) { id stream { id viewersCount } } }\",\n" +
                        "      \"operationName\":\"UseViewCount\",\n" +
                        "      \"variables\":{\n" +
                        "         \"channelLogin\":\"" + streamSource.url + "\"\n" +
                        "      },\n" +
                        "      \"extensions\":{\n" +
                        "         \"persistedQuery\":{\n" +
                        "            \"sha256Hash\":\"00b11c9c428f79ae228f30080a06ffd8226a1f068d6f52fbc057cbde66e994c2\",\n" +
                        "            \"version\":1\n" +
                        "         }\n" +
                        "      }\n" +
                        "   },\n" +
                        "   {\n" +
                        "      \"extensions\":{\n" +
                        "         \"persistedQuery\":{\n" +
                        "            \"sha256Hash\":\"639d5f11bfb8bf3053b424d9ef650d04c4ebb7d94711d644afb08fe9a0fad5d9\",\n" +
                        "            \"version\":1\n" +
                        "         }\n" +
                        "      },\n" +
                        "      \"query\":\"query UseLive(\$channelLogin: String!) { user(login: \$channelLogin) { id login stream { id createdAt } }\",\n" +
                        "      \"operationName\":\"UseLive\",\n" +
                        "      \"variables\":{\n" +
                        "         \"channelLogin\":\"" + streamSource.url + "\"\n" +
                        "      }\n" +
                        "   },\n" +
                        "   {\n" +
                        "      \"extensions\":{\n" +
                        "         \"persistedQuery\":{\n" +
                        "            \"sha256Hash\":\"0828119ded1c13477966434e15800ff57ddacf13ba1911c129dc2200705b0712\",\n" +
                        "            \"version\":1\n" +
                        "         }\n" +
                        "      },\n" +
                        "      \"operationName\":\"PlaybackAccessToken\",\n" +
                        "      \"variables\":{\n" +
                        "         \"isLive\":true,\n" +
                        "         \"isVod\":false,\n" +
                        "         \"login\":\"" + streamSource.url + "\",\n" +
                        "         \"playerType\":\"frontpage\",\n" +
                        "         \"vodID\":\"\"\n" +
                        "      },\n" +
                        "      \"query\":\"query PlaybackAccessToken(\$login: String! \$isLive: Boolean! \$vodID: ID! \$isVod: Boolean! \$playerType: String!) { streamPlaybackAccessToken(channelName: \$login params: {platform: \\\"web\\\" playerBackend: \\\"mediaplayer\\\" playerType: \$playerType}) @include(if: \$isLive) { value signature } videoPlaybackAccessToken(id: \$vodID params: {platform: \\\"web\\\" playerBackend: \\\"mediaplayer\\\" playerType: \$playerType}) @include(if: \$isVod) { value signature } }\"\n" +
                        "   }\n" +
                        "]".trimIndent()

                val data = getJSONFromURL(gqlUrl, method = "POST", headers = headers, body = gql)

                val id = streamSource.url
                val accessTokenValue = JsonPath.parse(data)?.read<String>("[3].data.streamPlaybackAccessToken.value")
                val accessTokenSignature = JsonPath.parse(data)?.read<String>("[3].data.streamPlaybackAccessToken.signature")

                url = "https://usher.ttvnw.net/api/channel/hls/${id}.m3u8?acmb=e30=&allow_source=true&fast_bread=true&p=&play_session_id=&player_backend=mediaplayer&playlist_include_framerate=true&reassignments_supported=true&sig=${accessTokenSignature}&supported_codecs=avc1&token=${accessTokenValue}&transcode_mode=vbr_v1&cdm=wv&player_version=1.20.0"
                //url ="https://usher.ttvnw.net/api/channel/hls/${id}.m3u8?client_id=${clientId}&token=${accessTokenValue}&sig=${accessTokenSignature}&allow_source=true&allow_audio_only=true"
            }
            else if (streamSource.streamSourceType == StreamSourceTypeItem.YOUTUBE) {
                try{
                    val html = fetchHtmlFromUrl("https://www.youtube.com/@${streamSource.url}/live")

                    val regex = """"hlsManifestUrl":"(https://[^"]+\.m3u8)"""".toRegex()
                    val matchResult = regex.find(html)
                    url = if (matchResult == null) {
                        ""
                    } else{
                        matchResult?.groups?.get(1)?.value
                    }
                }
                catch (e: Exception) {
                    url = ""
                }
            }
            else {
                url = streamSource.url
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

        private fun getHeadersMapFromApiCallHeadersObject(headers: List<ApiCallHeaderItem>) : Map<String, String> {
            val headersMap = mutableMapOf<String, String>()
            for (header in headers) {
                headersMap[header.key] = header.value
            }
            return headersMap
        }

        private fun fetchHtmlFromUrl(urlString: String): String {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val inputStream = connection.inputStream
            val scanner = Scanner(inputStream).useDelimiter("\\A")
            val htmlContent = if (scanner.hasNext()) scanner.next() else ""

            connection.disconnect()
            return htmlContent
        }
    }

}