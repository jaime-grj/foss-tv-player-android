package com.gaarx.tvplayer.ui.handler

import android.util.Log
import android.view.View
import com.gaarx.tvplayer.databinding.FragmentPlayerBinding
import com.gaarx.tvplayer.domain.model.ChannelItem
import com.gaarx.tvplayer.ui.view.PlayerFragment.Companion.TAG
import com.gaarx.tvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.tvplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

class FastSwitchHandler(
    private val playerViewModel: PlayerViewModel,
    private val channelViewModel: ChannelViewModel,
    private val binding: FragmentPlayerBinding,
    private val uiScope: CoroutineScope,
    private val onLoadChannel: (ChannelItem) -> Unit
) {
    var isLongPressDown = false
        private set
    var isLongPressUp = false
        private set

    private var jobFastChangeChannel: Job? = null

    fun handleKeyUp(): Boolean {
        if (isLongPressDown || isLongPressUp) {
            channelViewModel.updateIsLoadingChannel(true)
            val wasUp = isLongPressUp
            val wasDown = isLongPressDown
            
            isLongPressUp = false
            isLongPressDown = false

            uiScope.launch {
                jobFastChangeChannel?.cancelAndJoin()
                
                val currentCategoryId = playerViewModel.currentCategoryId.value ?: -1L
                val channelIdToSwitch = playerViewModel.channelIdFastSwitch.value!!
                
                Log.i(TAG, "After pressing long button: get newChannelId: $channelIdToSwitch")
                val newChannel = channelViewModel.getNextChannel(currentCategoryId, channelIdToSwitch)
                Log.i(TAG, "After pressing long button: load newChannelId: $channelIdToSwitch")
                
                onLoadChannel(newChannel)
                channelViewModel.updateIsLoadingChannel(false)
            }
            return true
        }
        return false
    }

    fun handleDpadUp(repeatCount: Int): Boolean {
        if (repeatCount > 0) {
            isLongPressUp = true
            startFastSwitch(isUp = true)
            return true
        }
        return false
    }

    fun handleDpadDown(repeatCount: Int): Boolean {
        if (repeatCount > 0) {
            isLongPressDown = true
            startFastSwitch(isUp = false)
            return true
        }
        return false
    }

    fun isLongPressActive() = isLongPressUp || isLongPressDown

    private fun startFastSwitch(isUp: Boolean) {
        binding.channelNumber.visibility = View.VISIBLE
        binding.channelName.visibility = View.INVISIBLE
        binding.channelMediaInfo.visibility = View.GONE

        val previousJob = jobFastChangeChannel
        jobFastChangeChannel = uiScope.launch {
            previousJob?.cancelAndJoin()
            
            val categoryId = playerViewModel.currentCategoryId.value ?: -1L
            val currentIndex = playerViewModel.channelIdFastSwitch.value!!
            
            val nextIndex = if (isUp) {
                channelViewModel.getNextChannelIndex(categoryId, currentIndex)
            } else {
                channelViewModel.getPreviousChannelIndex(categoryId, currentIndex)
            }
            
            playerViewModel.updateChannelIdFastSwitch(nextIndex)
            binding.channelNumber.text = nextIndex.toString()
        }
    }
}
