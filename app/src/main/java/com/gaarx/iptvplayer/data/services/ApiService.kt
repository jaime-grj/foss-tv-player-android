package com.gaarx.iptvplayer.data.services

import android.util.Log
import com.gaarx.iptvplayer.data.services.http.HttpClient
import com.gaarx.iptvplayer.domain.model.ApiCallHeaderItem
import com.gaarx.iptvplayer.domain.model.DrmHeaderItem
import com.gaarx.iptvplayer.domain.model.DrmTypeItem
import com.gaarx.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarx.iptvplayer.domain.model.StreamSourceItem
import com.gaarx.iptvplayer.domain.model.StreamSourceTypeItem
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import java.util.regex.Pattern

class ApiService (
    private val httpClient: HttpClient = HttpClient()
){

    companion object {
        private const val TAG = "ApiService"
    }

    private fun getURLFromHTML(url: String, stringSearch: String): String {
        return try {
            // Fetch and parse the document
            val doc: Document = Jsoup.connect(url).get()

            // Extract the entire HTML as a single text string
            val htmlText = doc.html()

            // Regular expression to match URLs
            val regex = """https?://[^\s"'<>]+"""
            val urlPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)

            // Search for URLs using the regex pattern
            val matcher = urlPattern.matcher(htmlText)
            while (matcher.find()) {
                val foundUrl = matcher.group()
                // Check if the found URL contains the search string
                if (foundUrl.contains(stringSearch)) {
                    return foundUrl
                }
            }

            "" // No matching URL found
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getURLFromChannelSource(streamSource: StreamSourceItem): String? {
        var finalUrl: String?
        if (streamSource.streamSourceType == StreamSourceTypeItem.IPTV) {
            if (streamSource.apiCalls.isNullOrEmpty()) {
                return streamSource.url
            }

            val apiCalls = streamSource.apiCalls.sortedBy { it.index }
            val variableStore = mutableMapOf<String, String>()

            for (apiCall in apiCalls) {
                var url = apiCall.url
                val rawHeaders = apiCall.headers?.let { getHeadersMapFromApiCallHeadersObject(it) } ?: emptyMap()
                val headers = rawHeaders.mapValues { (_, v) -> replacePlaceholders(v, variableStore) }
                val method = apiCall.method
                var body = apiCall.body
                val apiResponseKeys = apiCall.apiResponseKeys

                // Replace placeholders {{key}} in URL and body
                url = replacePlaceholders(url, variableStore)
                body = body?.let { replacePlaceholders(it, variableStore) }

                Log.d("ApiService", "Processed URL: $url")
                Log.d("ApiService", "Processed Body: $body")

                when (apiCall.type) {
                    "json" -> {
                        val json = httpClient.request(url, method ?: "GET", headers, body)
                        apiResponseKeys.forEach { apiResponseKey ->
                            val jsonPath = apiResponseKey.jsonPath
                            val storeKey = apiResponseKey.storeKey
                            val extractedValue = JsonPath.parse(json)?.read<String>(jsonPath)
                            if (!storeKey.isNullOrEmpty() && !extractedValue.isNullOrEmpty()) {
                                variableStore[storeKey] = extractedValue
                                Log.d("ApiService", "Stored: $storeKey = $extractedValue")
                            }
                        }
                    }
                    "html" -> {
                        finalUrl = getURLFromHTML(url, apiCall.stringSearch!!)
                        return finalUrl
                    }
                }
            }

            finalUrl = replacePlaceholders(streamSource.url, variableStore)
            Log.d("ApiService", "Final URL: $finalUrl")
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

            val data = httpClient.post(gqlUrl, headers, gql)

            val id = streamSource.url
            val accessTokenValue = JsonPath.parse(data)?.read<String>("[3].data.streamPlaybackAccessToken.value")
            val accessTokenSignature = JsonPath.parse(data)?.read<String>("[3].data.streamPlaybackAccessToken.signature")

            finalUrl = "https://usher.ttvnw.net/api/channel/hls/${id}.m3u8?acmb=e30=&allow_source=true&fast_bread=true&p=&play_session_id=&player_backend=mediaplayer&playlist_include_framerate=true&reassignments_supported=true&sig=${accessTokenSignature}&supported_codecs=avc1&token=${accessTokenValue}&transcode_mode=vbr_v1&cdm=wv&player_version=1.20.0"
        }
        else if (streamSource.streamSourceType == StreamSourceTypeItem.YOUTUBE) {
            try{
                val html = fetchHtmlFromUrl("https://www.youtube.com/${streamSource.url}/live")

                val regexHls = """"hlsManifestUrl":"(https://[^"]+\.m3u8)"""".toRegex()
                val regexDash = """"dashManifestUrl":"(https://[^"]+)"""".toRegex()
                val matchResult = regexHls.find(html)
                finalUrl = if (matchResult == null) {
                    ""
                } else{
                    matchResult.groups[1]?.value
                }
            }
            catch (e: Exception) {
                finalUrl = ""
            }
        }
        else {
            finalUrl = streamSource.url
        }

        return finalUrl
    }

    fun replacePlaceholders(text: String, variables: Map<String, String>): String {
        var result = text
        val regex = Regex("\\{\\{(.+?)\\}\\}") // Matches {{key}}
        regex.findAll(text).forEach { match ->
            val key = match.groupValues[1]
            val value = variables[key] ?: ""
            result = result.replace(match.value, value)
        }
        return result
    }

    fun getHeadersMapFromHeadersObject(headers: List<StreamSourceHeaderItem>): Map<String, String> =
        headers.associate { it.key to it.value }

    fun getDrmKeys(streamSource: StreamSourceItem): String {
        if (streamSource.drmType == DrmTypeItem.LICENSE) {
            Log.i("ApiService", "getDrmKeys")
            val headers = streamSource.drmHeaders!!.joinToString(",") { "'${it.key}': '${it.value}'" }
            Log.i("ApiService", "getDrmKeys - headers: $headers")
            return httpClient.post(
                url = "http://cdrm.zhnx.home.arpa/api/decrypt",
                headers = mapOf(
                    "Content-Type" to "application/json"
                ),
                body = """
                {
                    "PSSH": "${streamSource.pssh}",
                    "License URL": "${streamSource.licenseUrl}",
                    "Headers": "{$headers}",
                    "JSON": "{}",
                    "Cookies": "{}",
                    "Data": "{}",
                    "Proxy": ""
                }
            """.trimIndent()
            )
        }
        else {
            Log.i("ApiService", "getDrmKeys - no drm")
            return ""
        }
    }

    private fun getHeadersMapFromApiCallHeadersObject(headers: List<ApiCallHeaderItem>): Map<String, String> =
        headers.associate { it.key to it.value }

    private fun getHeadersMapFromDrmHeadersObject(headers: List<DrmHeaderItem>): Map<String, String> =
        headers.associate { it.key to it.value }

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