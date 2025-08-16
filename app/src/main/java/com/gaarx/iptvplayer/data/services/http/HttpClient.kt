package com.gaarx.iptvplayer.data.services.http

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class HttpClient(
    private val connectTimeout: Int = 5000
) {

    companion object {
        private const val TAG = "HttpClient"
    }

    fun request(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): String {
        var connection: HttpURLConnection? = null
        return try {
            val urlObj = URL(url)
            connection = (urlObj.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = this@HttpClient.connectTimeout
                addRequestProperty("Host", urlObj.host)

                headers.forEach { (key, value) ->
                    addRequestProperty(key, value)
                }

                if (method == "POST" && body != null) {
                    doOutput = true
                    doInput = true
                    val outputStream: OutputStream = outputStream
                    outputStream.write(body.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                    outputStream.close()
                }
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    buildString {
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            append(line)
                        }
                    }
                }
            } else {
                Log.e(TAG, "Request failed: $responseCode")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request error", e)
            ""
        } finally {
            connection?.disconnect()
        }
    }

    fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        return request(url, "GET", headers)
    }

    fun post(url: String, headers: Map<String, String> = emptyMap(), body: String? = null): String {
        return request(url, "POST", headers, body)
    }
}