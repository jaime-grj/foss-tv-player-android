package com.gaarx.iptvplayer.data.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class DataService @Inject constructor() {

    companion object {

        private const val CONNECT_TIMEOUT = 5000
        private const val DATA_URL = "http://filehost.zhnx.home.arpa/channels.json"

        suspend fun getJSONString(): String {
            var json = ""
            var connection: HttpURLConnection? = null
            try {

                val urlObj =
                    URL(DATA_URL)
                connection = withContext(Dispatchers.IO) {
                    urlObj.openConnection()
                } as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.addRequestProperty("Host", urlObj.host)

                withContext(Dispatchers.IO) {
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
                    }else {
                        Log.e("DataService", "Request failed: $responseCode")
                    }
                }

            }catch (e: Exception) {
                e.printStackTrace()
                return ""
            } finally {
                connection?.disconnect()
            }
            return json
        }
    }
}