package com.gaarx.iptvplayer.ui.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.gaarx.iptvplayer.core.Constants.BUFFERING_TIMEOUT_MS
import com.gaarx.iptvplayer.core.Constants.CHANNEL_LOADING_TIMEOUT_MS
import com.gaarx.iptvplayer.core.Constants.HIDE_PLAYER_TIMEOUT_MS
import com.gaarx.iptvplayer.core.Constants.PLAYING_TIMEOUT_MS
import com.gaarx.iptvplayer.core.Constants.SOURCE_LOADING_TIMEOUT_MS
import com.gaarx.iptvplayer.ui.view.PlayerFragment

class PlayerTimerManager {

    companion object {
        private const val TAG = "PlayerTimerManager"
    }

    private val handler = Handler(Looper.getMainLooper())

    private var hidePlayerRunnable: Runnable? = null
    private var sourceLoadingRunnable: Runnable? = null
    private var bufferingRunnable: Runnable? = null
    private var checkPlayingRunnable: Runnable? = null
    private var loadingIndicatorRunnable: Runnable? = null

    fun startHidePlayerTimer(delayMillis: Long = HIDE_PLAYER_TIMEOUT_MS, action: () -> Unit) {
        cancelHidePlayerTimer()
        hidePlayerRunnable = Runnable {
            Log.i(TAG, "Executing hide player timer")
            action()
        }
        handler.postDelayed(hidePlayerRunnable!!, delayMillis)
        Log.i(TAG, "Started hide player timer ($delayMillis ms)")
    }

    fun cancelHidePlayerTimer() {
        hidePlayerRunnable?.let {
            handler.removeCallbacks(it)
            Log.i(TAG, "Cancelled hide player timer")
        }
    }

    fun startSourceLoadingTimer(
        delayMillis: Long = SOURCE_LOADING_TIMEOUT_MS,
        onTimeout: () -> Unit
    ) {
        cancelSourceLoadingTimer()
        sourceLoadingRunnable = Runnable {
            Log.i(TAG, "Executing source loading timeout")
            onTimeout()
        }
        handler.postDelayed(sourceLoadingRunnable!!, delayMillis)
        Log.i(TAG, "Started source loading timer ($delayMillis ms)")
    }

    fun cancelSourceLoadingTimer() {
        sourceLoadingRunnable?.let {
            handler.removeCallbacks(it)
            Log.i(TAG, "Cancelled source loading timer")
        }
    }

    fun startBufferingTimer(delayMillis: Long = BUFFERING_TIMEOUT_MS, onTimeout: () -> Unit) {
        cancelBufferingTimer()
        bufferingRunnable = Runnable {
            Log.i(TAG, "Executing buffering timeout")
            onTimeout()
        }
        handler.postDelayed(bufferingRunnable!!, delayMillis)
        Log.i(TAG, "Started buffering timer ($delayMillis ms)")
    }

    fun cancelBufferingTimer() {
        bufferingRunnable?.let {
            handler.removeCallbacks(it)
            Log.i(TAG, "Cancelled buffering timer")
        }
    }

    // ─────────────────────────────────────
    // Check Playing Correctly
    fun startCheckPlayingCorrectlyTimer(delayMillis: Long = PLAYING_TIMEOUT_MS, onTimeout: () -> Unit) {
        cancelCheckPlayingCorrectlyTimer()
        checkPlayingRunnable = Runnable {
            Log.i(TAG, "Executing check playing correctly timeout")
            onTimeout()
        }
        handler.postDelayed(checkPlayingRunnable!!, delayMillis)
        Log.i(TAG, "Started check playing correctly timer ($delayMillis ms)")
    }

    fun cancelCheckPlayingCorrectlyTimer() {
        checkPlayingRunnable?.let {
            handler.removeCallbacks(it)
            Log.i(TAG, "Cancelled check playing correctly timer")
        }
    }

    // ─────────────────────────────────────
    // Channel Loading Indicator
    fun startLoadingIndicatorTimer(delayMillis: Long = 2500L, onTimeout: () -> Unit) {
        cancelLoadingIndicatorTimer()
        loadingIndicatorRunnable = Runnable {
            Log.i(TAG, "Executing channel loading indicator timeout")
            onTimeout()
        }
        handler.postDelayed(loadingIndicatorRunnable!!, delayMillis)
        Log.i(TAG, "Started loading indicator timer ($delayMillis ms)")
    }

    fun cancelLoadingIndicatorTimer() {
        loadingIndicatorRunnable?.let {
            handler.removeCallbacks(it)
            Log.i(TAG, "Cancelled loading indicator timer")
        }
    }

/*    @OptIn(UnstableApi::class)
    private fun cancelTryNextStreamSourceTimer() {
        Log.i(PlayerFragment.Companion.TAG, "cancelTryNextStreamSourceTimer")
        handler.removeCallbacks(tryNextStreamSourceRunnable)
    }

    private val tryNextStreamSourceRunnable = Runnable {
        Log.i(PlayerFragment.Companion.TAG, "Try Next Stream Source")
        val currentChannel = channelViewModel.currentChannel.value ?: return@Runnable
        val currentStreamSource = playerViewModel.currentStreamSource.value ?: return@Runnable
        tryNextStreamSource(currentChannel, currentStreamSource)
    }*/
}