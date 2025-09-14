package com.gaarx.tvplayer.ui.util

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.gaarx.tvplayer.ui.view.PlayerTimerManager

class PlayerLifecycleManager(
    private val timerManager: PlayerTimerManager
) : LifecycleObserver {

    companion object {
        private const val TAG = "PlayerLifecycleManager"
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.i(TAG, "Fragment stopped — canceling all timers")
        cancelAllTimers()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.i(TAG, "Fragment destroyed — canceling all timers")
        cancelAllTimers()
    }

    fun cancelAllTimers() {
        timerManager.cancelHidePlayerTimer()
        timerManager.cancelBufferingTimer()
        timerManager.cancelCheckPlayingCorrectlyTimer()
        timerManager.cancelSourceLoadingTimer()
        timerManager.cancelLoadingIndicatorTimer()
    }
}
