package com.gaarx.iptvplayer.ui.view

import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.gaarx.iptvplayer.core.Constants.TIME_CACHED_URL_MINUTES
import com.gaarx.iptvplayer.core.Constants.TRIES_EACH_SOURCE
import com.gaarx.iptvplayer.data.services.ApiService
import com.gaarx.iptvplayer.domain.model.ChannelItem
import com.gaarx.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarx.iptvplayer.domain.model.StreamSourceItem
import com.gaarx.iptvplayer.domain.model.StreamSourceTypeItem
import com.gaarx.iptvplayer.ui.util.PlayerTimerManager
import com.gaarx.iptvplayer.ui.view.PlayerFragment.Companion.TAG
import com.gaarx.iptvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.iptvplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ProxySelector
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class StreamSourceManager(
    private val playerManager: PlayerManager,
    private val playerViewModel: PlayerViewModel,
    private val channelViewModel: ChannelViewModel,
    private val timerManager: PlayerTimerManager,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    data class CachedUrl(val url: String, val timestamp: Long)
    private val urlCache: MutableMap<StreamSourceItem, CachedUrl> = ConcurrentHashMap()
    private val cacheExpirationTime = TimeUnit.MINUTES.toMillis(TIME_CACHED_URL_MINUTES)

    private val player: ExoPlayer
        get() = playerManager.exoPlayer

    fun loadStreamSource(streamSource: StreamSourceItem) {
        timerManager.cancelSourceLoadingTimer()
        playerViewModel.setIsSourceLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            val cachedUrlObj = urlCache[streamSource]
            val currentTime = System.currentTimeMillis()

            if (streamSource.proxies != null) {
                if (streamSource.proxies.isEmpty()) {
                    Log.i(TAG, "No proxies, reset")
                    //ProxySelector.setDefault(originalProxySelector)
                } else {
                    Log.i(TAG, "Found proxy: ${streamSource.proxies.first()}")
                    val proxy = streamSource.proxies.first()
                    //ProxySelector.setDefault(CustomProxySelector(proxy.hostname, proxy.port))
                }
            }

            val url =
                if (cachedUrlObj != null && (currentTime - cachedUrlObj.timestamp) < cacheExpirationTime) {
                    cachedUrlObj.url
                } else {
                    val newUrl = ApiService.getURLFromChannelSource(streamSource)!!
                    urlCache[streamSource] = CachedUrl(newUrl, currentTime)
                    newUrl
                }

            val headersObj: List<StreamSourceHeaderItem> = streamSource.headers ?: emptyList()
            val headers: MutableMap<String, String> = when (streamSource.streamSourceType) {
                StreamSourceTypeItem.IPTV -> {
                    ApiService.getHeadersMapFromHeadersObject(headersObj)
                }

                StreamSourceTypeItem.TWITCH -> {
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0",
                        "Accept" to "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain",
                        "Accept-Language" to "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3",
                        "Accept-Encoding" to "gzip, deflate, br, zstd",
                        "Referer" to "https://www.twitch.tv/",
                        "Origin" to "https://www.twitch.tv",
                        "Connection" to "keep-alive",
                        "Sec-Fetch-Dest" to "empty",
                        "Sec-Fetch-Mode" to "cors",
                        "Sec-Fetch-Site" to "cross-site",
                        "Priority" to "u=4"
                    )
                }

                else -> {
                    mapOf()
                }
            }.toMutableMap()

            if (headers["User-Agent"] == null) {
                headers["User-Agent"] =
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
            }

            Log.i(TAG, "Stream URL: $url")
            Log.i(TAG, "Headers: $headers")

            // Now hand off to playerManager to load into player
            playerManager.prepareMediaSource(streamSource, headers, url)

            // Start timeout
            timerManager.startSourceLoadingTimer {
                val channel = channelViewModel.currentChannel.value
                val source = playerViewModel.currentStreamSource.value
                if (channel != null && source != null) {
                    tryNextStreamSource(
                        channel,
                        source
                    ) // You can move this here or keep in fragment
                }
            }
        }
    }

    fun tryNextStreamSource(
        currentChannel: ChannelItem,
        currentStreamSource: StreamSourceItem
    ) {
        Log.i(TAG, "Trying next stream source")

        cancelTimers()

        if (player.isPlaying || player.isLoading) {
            player.stop()
        }

        if (playerViewModel.isSourceForced.value == true) {
            retryForcedSource(currentStreamSource)
        } else {
            retryAutoSelectSource(currentChannel, currentStreamSource)
        }
    }

    private fun cancelTimers() {
        timerManager.cancelBufferingTimer()
        timerManager.cancelSourceLoadingTimer()
        timerManager.cancelCheckPlayingCorrectlyTimer()
    }

    private fun retryForcedSource(streamSource: StreamSourceItem) {
        playerViewModel.updateTriesCountForEachSource(
            playerViewModel.triesCountForEachSource.value?.plus(1) ?: 1
        )

        if ((playerViewModel.triesCountForEachSource.value ?: 0) > TRIES_EACH_SOURCE) {
            playerViewModel.hideMediaInfo()
            playerViewModel.hidePlayer()
            playerViewModel.showErrorMessage()
            playerViewModel.hideAnimatedLoadingIcon()
            playerViewModel.updateTriesCountForEachSource(0)
        } else {
            lifecycleScope.launch {
                loadStreamSource(streamSource)
            }
        }
    }

    private fun retryAutoSelectSource(channel: ChannelItem, currentStreamSource: StreamSourceItem) {
        val streamSources = channel.streamSources.sortedBy { it.index }
        val currentIndex = currentStreamSource.index

        val nextSource = streamSources.firstOrNull { it.index > currentIndex }
            ?: streamSources.firstOrNull()

        if (nextSource == null) {
            showFinalError()
        }

        val updatedTries = (playerViewModel.triesCountForEachSource.value ?: 0) + 1
        playerViewModel.updateTriesCountForEachSource(updatedTries)

        if (updatedTries > TRIES_EACH_SOURCE) {
            val sourcesTried = (playerViewModel.sourcesTriedCount.value ?: 0) + 1
            playerViewModel.updateSourcesTriedCount(sourcesTried)
            playerViewModel.updateTriesCountForEachSource(1)

            if (sourcesTried >= streamSources.size) {
                showFinalError()
            }
        }

        lifecycleScope.launch {
            loadStreamSource(nextSource!!)
        }
    }

    private fun showFinalError() {
        playerViewModel.hidePlayer()
        playerViewModel.showErrorMessage()
        playerViewModel.hideAnimatedLoadingIcon()
    }
}
