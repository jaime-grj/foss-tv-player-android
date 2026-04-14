package com.gaarx.tvplayer.data.services.http

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Proxy
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
        body: String? = null,
        proxy: Proxy? = null
    ): String {
        var connection: HttpURLConnection? = null
        return try {
            val urlObj = URL(url)
            connection = (if (proxy != null) urlObj.openConnection(proxy) else urlObj.openConnection()) as HttpURLConnection
            connection.apply {
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

    fun get(url: String, headers: Map<String, String> = emptyMap(), proxy: Proxy? = null): String {
        return request(url, "GET", headers, proxy = proxy)
    }

    fun post(url: String, headers: Map<String, String> = emptyMap(), body: String? = null, proxy: Proxy? = null): String {
        return request(url, "POST", headers, body, proxy)
    }
}