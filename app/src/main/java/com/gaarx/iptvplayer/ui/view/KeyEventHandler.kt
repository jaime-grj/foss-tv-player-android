package com.gaarx.iptvplayer.ui.view

import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.iptvplayer.core.Constants.MAX_DIGITS
import com.gaarx.iptvplayer.core.Constants.TIMEOUT_UI_INFO
import com.gaarx.iptvplayer.databinding.FragmentPlayerBinding
import com.gaarx.iptvplayer.domain.model.*
import com.gaarx.iptvplayer.exceptions.ChannelNotFoundException
import com.gaarx.iptvplayer.ui.adapters.*
import com.gaarx.iptvplayer.ui.view.PlayerFragment.Companion.TAG
import com.gaarx.iptvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.iptvplayer.ui.viewmodel.PlayerViewModel
import com.gaarx.iptvplayer.util.DeviceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch


class KeyEventHandler @OptIn(UnstableApi::class) constructor
    (
    private val binding: FragmentPlayerBinding,
    private val context: android.content.Context,
    private val playerViewModel: PlayerViewModel,
    private val channelViewModel: ChannelViewModel,
    private val rvChannelList: VerticalGridView,
    private val rvChannelSettings: RecyclerView,
    private val rvCategoryList: VerticalGridView,
    private val rvAudioTracks: VerticalGridView,
    private val rvSubtitlesTracks: VerticalGridView,
    private val rvChannelSources: VerticalGridView,
    private val rvVideoTracks: VerticalGridView,
    private val rvNumberList: RecyclerView,
    private val playerManager: PlayerManager,
    private val uiScope: CoroutineScope,
    private val onLoadChannel: (ChannelItem) -> Unit,
    private val onLoadNextChannel: () -> Unit,
    private val onLoadPreviousChannel: () -> Unit,
    private val onLoadStreamSource: (StreamSourceItem) -> Unit,
    private val onShowChannelInfoWithTimeout: () -> Unit,
    private val onShowFullChannelUIWithTimeout: (Long) -> Unit,
    private val onShowChannelNumberWithTimeoutAndChangeChannel: () -> Unit,
    private val onLoadSetting: (Int) -> Unit,
    private val onInitSettingsMenu: () -> Unit,
    private val onInitChannelList: suspend () -> Unit,
    private val activity: FragmentActivity
) {
    var isLongPressDown = false
    var isLongPressUp = false
    var channelIdFastSwitch = 0 /*if (channelViewModel.currentCategoryId.value == -1L) {
        uiScope.launch { channelViewModel.getLastChannelLoaded()
            channelViewModel.currentChannel.value?.indexFavourite ?: 0 }
    }
    else {
        channelViewModel.currentChannel.value?.indexGroup!!
    }*/

    // Jobs moved from PlayerFragment
    private lateinit var jobFastChangeChannel: kotlinx.coroutines.Job
    private lateinit var jobUIChangeChannel: kotlinx.coroutines.Job

    @OptIn(UnstableApi::class)
    fun handle(event: KeyEvent): Boolean {
        if (channelViewModel.isLoadingChannelList.value == true
            || channelViewModel.isImportingData.value == true
            || channelViewModel.isLoadingChannel.value == true) return true
        val code = event.keyCode
        val action = event.action
        val isDown = action == 0

        if (!isDown){
            if (isLongPressDown || isLongPressUp){
                channelViewModel.updateIsLoadingChannel(true)
                if (::jobFastChangeChannel.isInitialized && jobFastChangeChannel.isActive){
                    uiScope.launch {
                        jobFastChangeChannel.cancelAndJoin()
                    }
                }
                isLongPressUp = false
                isLongPressDown = false
                if (channelViewModel.currentCategoryId.value == -1L){
                    jobFastChangeChannel = uiScope.launch {
                        Log.i(TAG, "After pressing long button: get newChannelId: $channelIdFastSwitch")
                        val newChannel = channelViewModel.getNextChannel(-1L, channelIdFastSwitch)
                        Log.i(TAG, "After pressing long button: load newChannelId: $channelIdFastSwitch")
                        channelViewModel.updateIsLoadingChannel(false)
                        onLoadChannel(newChannel)
                    }
                }
                else{
                    jobFastChangeChannel = uiScope.launch {
                        val newChannel = channelViewModel.getNextChannel(channelViewModel.currentCategoryId.value!!, channelIdFastSwitch)
                        onLoadChannel(newChannel)
                        channelViewModel.updateIsLoadingChannel(false)
                    }
                }
            }
            else{
                return false
            }
        }
        else{
            when (code) {
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    if (isLongPressDown || isLongPressUp) return true
                    // Load channel from channel list
                    if (playerViewModel.isChannelListVisible.value == true) {
                        val currentChannelIndex = if (channelViewModel.currentCategoryId.value == -1L) {
                            channelViewModel.currentChannel.value?.indexFavourite!!
                        } else {
                            channelViewModel.currentChannel.value?.indexGroup!!
                        }
                        Log.i(TAG, "dispatchKeyEvent: currentChannelIndex: $currentChannelIndex")
                        val focusedView = rvChannelList.focusedChild
                        if (focusedView != null) {
                            val position = rvChannelList.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            Log.i(
                                TAG,
                                "dispatchKeyEvent: currentItemSelectedFromChannelList: $position"
                            )
                            val newChannelSm =
                                (rvChannelList.adapter as? ChannelListAdapter)?.getItemAtPosition(
                                    position
                                ) as ChannelItem
                            var newChannel: ChannelItem?
                            if (channelViewModel.currentCategoryId.value == -1L) {
                                uiScope.launch {
                                    newChannel = channelViewModel.getNextChannel(
                                        -1L,
                                        newChannelSm.indexFavourite!!
                                    )
                                    onLoadChannel(newChannel)
                                }
                            } else {
                                println("newChannelSm.indexFavourite: ${newChannelSm.indexFavourite}, channelViewModel.currentCategoryId.value: ${channelViewModel.currentCategoryId.value}")
                                uiScope.launch {
                                    newChannel = channelViewModel.getNextChannel(
                                        channelViewModel.currentCategoryId.value!!,
                                        newChannelSm.indexGroup!!
                                    )
                                    onLoadChannel(newChannel)
                                }
                            }
                        }
                        else{
                            channelViewModel.updateIsLoadingChannel(false)
                            onShowChannelInfoWithTimeout()
                        }
                    }
                    else if (playerViewModel.isCategoryListVisible.value == true) {
                        channelViewModel.updateIsLoadingChannel(true)
                        val focusedView = rvCategoryList.focusedChild
                        if (focusedView != null) {
                            val position = rvCategoryList.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val newCategory = (rvCategoryList.adapter as? CategoryListAdapter)?.getItemAtPosition(position) as CategoryItem
                            channelViewModel.updateCurrentCategoryId(newCategory.id)
                            playerViewModel.updateCategoryName(newCategory.name)
                            uiScope.launch {
                                onLoadChannel(channelViewModel.getNextChannel(categoryId = newCategory.id, groupId = 1))
                                channelViewModel.updateIsLoadingChannel(false)
                                Log.i(TAG, "dispatchKeyEvent: newCategory: $newCategory")
                                channelViewModel.updateLastCategoryLoaded(newCategory.id)
                            }

                            playerViewModel.hideChannelName()
                            playerViewModel.hideCategoryName()
                            playerViewModel.hideChannelNumber()
                            playerViewModel.hideBottomInfo()
                            playerViewModel.hideMediaInfo()
                            playerViewModel.hideTimeDate()

                            playerViewModel.hideCategoryList()
                        }
                        else{
                            channelViewModel.updateIsLoadingChannel(false)
                            playerViewModel.hideCategoryList()
                        }
                    }
                    // Enter specific setting
                    else if (playerViewModel.isSettingsMenuVisible.value == true) {
                        val focusedView = rvChannelSettings.focusedChild
                        if (focusedView != null) {
                            val position = rvChannelSettings.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            playerViewModel.hideSettingsMenu()

                            when (position) {
                                ChannelSettings.AUDIO_TRACKS -> {
                                    onLoadSetting(ChannelSettings.AUDIO_TRACKS)
                                }
                                ChannelSettings.SUBTITLES_TRACKS -> {
                                    onLoadSetting(ChannelSettings.SUBTITLES_TRACKS)
                                }
                                ChannelSettings.SOURCES -> {
                                    onLoadSetting(ChannelSettings.SOURCES)
                                }
                                ChannelSettings.VIDEO_TRACKS -> {
                                    onLoadSetting(ChannelSettings.VIDEO_TRACKS)
                                }
                                ChannelSettings.UPDATE_EPG -> {
                                    onLoadSetting(ChannelSettings.UPDATE_EPG)
                                }
                                ChannelSettings.SHOW_EPG -> {
                                    onLoadSetting(ChannelSettings.SHOW_EPG)
                                }
                                ChannelSettings.UPDATE_CHANNEL_LIST -> {
                                    onLoadSetting(ChannelSettings.UPDATE_CHANNEL_LIST)
                                }
                            }
                        }
                        else{
                            playerViewModel.hideSettingsMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.AUDIO_TRACKS) {
                        val focusedView = rvAudioTracks.focusedChild
                        if (focusedView != null) {
                            val position = rvAudioTracks.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val audioTrack = (rvAudioTracks.adapter as? AudioTracksAdapter)?.getItemAtPosition(position) as AudioTrack
                            playerManager.loadAudioTrack(audioTrack)
                            playerViewModel.hideTrackMenu()
                        }
                        else{
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.SUBTITLES_TRACKS) {
                        val focusedView = rvSubtitlesTracks.focusedChild
                        if (focusedView != null) {
                            val position = rvSubtitlesTracks.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val subtitlesTrack = (rvSubtitlesTracks.adapter as? SubtitlesTracksAdapter)?.getItemAtPosition(position) as SubtitlesTrack
                            playerManager.loadSubtitlesTrack(subtitlesTrack)
                            playerViewModel.hideTrackMenu()
                        }
                        else{
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.SOURCES) {
                        val focusedView = rvChannelSources.focusedChild
                        if (focusedView != null) {
                            val position = rvChannelSources.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            if (playerViewModel.isErrorMessageVisible.value == true) {
                                playerViewModel.hideErrorMessage()
                            }
                            playerViewModel.hideBottomErrorMessage()
                            var streamSource = (rvChannelSources.adapter as? ChannelSourcesAdapter)?.getItemAtPosition(position) as StreamSourceItem
                            if (streamSource.id.toInt() == -1) {
                                playerViewModel.updateIsSourceForced(false)
                                val currentChannel = channelViewModel.currentChannel.value
                                if (currentChannel != null) {
                                    streamSource = currentChannel.streamSources.minBy { it.index }
                                }
                            }
                            else{
                                playerViewModel.updateIsSourceForced(true)
                            }
                            playerViewModel.updateTriesCountForEachSource(0)
                            playerViewModel.updateSourcesTriedCount(0)
                            playerViewModel.hideAnimatedLoadingIcon()
                            onLoadStreamSource(streamSource)
                            playerViewModel.hideTrackMenu()
                            playerViewModel.hidePlayer()
                        }
                        else{
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.VIDEO_TRACKS) {
                        val focusedView = rvChannelSources.focusedChild
                        if (focusedView != null) {
                            val position = rvChannelSources.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val videoTrack = (rvVideoTracks.adapter as? VideoTracksAdapter)?.getItemAtPosition(position) as VideoTrack
                            try{
                                if (videoTrack.id.toInt() == -1) {
                                    playerViewModel.updateIsQualityForced(false)
                                }
                                else{
                                    playerViewModel.updateIsQualityForced(true)
                                }
                            }
                            catch (_: Exception) {}
                            playerManager.loadVideoTrack(videoTrack)
                            playerViewModel.hideTrackMenu()
                        }
                        else{
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isNumberListMenuVisible.value == true) {
                        if (event.repeatCount > 0) {
                            return true
                        }
                        if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) {
                            jobUIChangeChannel.cancel()
                        }
                        if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                        if (playerViewModel.isChannelNameVisible.value == true) playerViewModel.hideChannelName()
                        if (playerViewModel.isChannelNumberVisible.value == true) playerViewModel.hideChannelNumber()
                        if (playerViewModel.isCategoryNameVisible.value == true) playerViewModel.hideCategoryName()
                        if (playerViewModel.getCurrentNumberInput().length >= MAX_DIGITS) {
                            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        }

                        val focusedView = rvNumberList.focusedChild
                        if (focusedView != null) {
                            val position = rvNumberList.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val selectedNumber = (rvNumberList.adapter as? NumberListAdapter)?.getItemAtPosition(position) as Int
                            val number = selectedNumber.toString()

                            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().append(number))

                            binding.channelNumberKeyboard.text = playerViewModel.getCurrentNumberInput().toString()

                            onShowChannelNumberWithTimeoutAndChangeChannel()

                        }
                        else{
                            playerViewModel.hideNumberListMenu()
                        }
                    }
                    else if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                        channelViewModel.updateIsLoadingChannel(true)
                        if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) jobUIChangeChannel.cancel()
                        val channelIndex = playerViewModel.getCurrentNumberInput().toString().toInt()
                        playerViewModel.hideChannelNumberKeyboard()
                        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        uiScope.launch {
                            try{
                                channelViewModel.updateIsLoadingChannel(true)
                                val newChannel = channelViewModel.getChannel(
                                    -1L,
                                    channelIndex
                                )
                                channelViewModel.updateCurrentCategoryId(-1L)
                                playerViewModel.updateCategoryName("Favoritos")
                                channelViewModel.updateLastCategoryLoaded(-1L)
                                if (channelViewModel.currentChannel.value?.id != newChannel.id) {
                                    onLoadChannel(newChannel)
                                }
                                else{
                                    onShowChannelInfoWithTimeout()
                                }
                                playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                                channelViewModel.updateIsLoadingChannel(false)
                            } catch (_: ChannelNotFoundException) {
                                playerViewModel.hideChannelNumberKeyboard()
                                playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                                channelViewModel.updateIsLoadingChannel(false)
                            } catch (_: kotlin.NumberFormatException) {
                                playerViewModel.hideChannelNumberKeyboard()
                                playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                                channelViewModel.updateIsLoadingChannel(false)
                            }
                        }
                    }
                    else if (playerViewModel.isChannelNumberVisible.value == true) {
                        if (event.repeatCount > 0) {
                            Log.i(TAG, "event repeat center")
                            if (playerViewModel.isNumberListMenuVisible.value != true) {
                                Log.i(TAG, "showNumberListMenu")
                                playerViewModel.hideMediaInfo()
                                playerViewModel.hideChannelName()
                                playerViewModel.hideChannelNumber()
                                playerViewModel.hideCategoryName()
                                playerViewModel.hideTimeDate()
                                playerViewModel.hideBottomInfo()
                                playerViewModel.showNumberListMenu()
                                rvNumberList.requestFocus()
                            }
                            return true
                        }
                        else{
                            if (!DeviceUtil.isAndroidTV(context)) {
                                playerViewModel.hideButtonUp()
                                playerViewModel.hideButtonDown()
                                playerViewModel.hideButtonChannelList()
                                playerViewModel.hideButtonSettings()
                                playerViewModel.hideButtonPiP()
                                playerViewModel.hideButtonCategoryList()
                            }
                            playerViewModel.hideChannelNumber()
                            playerViewModel.hideChannelName()
                            playerViewModel.hideCategoryName()
                            playerViewModel.hideTimeDate()
                            playerViewModel.hideMediaInfo()
                            playerViewModel.hideBottomInfo()
                        }
                        return true
                    }
                    // Show current channel info
                    else{
                        if (event.repeatCount > 0) {
                            Log.i(TAG, "event repeat center")
                            if (playerViewModel.isNumberListMenuVisible.value != true) {
                                Log.i(TAG, "showNumberListMenu")
                                playerViewModel.hideMediaInfo()
                                playerViewModel.hideChannelName()
                                playerViewModel.hideChannelNumber()
                                playerViewModel.hideCategoryName()
                                playerViewModel.hideTimeDate()
                                playerViewModel.hideBottomInfo()
                                playerViewModel.showNumberListMenu()
                                rvNumberList.requestFocus()
                            }
                            return true
                        }
                        else{
                            if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) {
                                jobUIChangeChannel.cancel()
                            }
                            if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                                playerViewModel.hideChannelNumberKeyboard()
                                playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                            }
                            onShowFullChannelUIWithTimeout(TIMEOUT_UI_INFO)
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (binding.channelList.isVisible
                        || binding.rvChannelSettings.isVisible
                        || binding.rvCategoryList.isVisible) { // Navigate through menu
                        return false
                    }
                    else if (binding.rvChannelTrackSettings.isVisible) {
                        return rvAudioTracks.adapter!!.itemCount == 0 || rvSubtitlesTracks.adapter!!.itemCount == 0
                    }
                    else if (binding.rvNumberList.isVisible) {
                        return true
                    } else{
                        if (event.repeatCount > 0){
                            //if (isLongPressDown) return true // Uncomment to disallow 'channel fast switch direction' change
                            isLongPressUp = true
                            if (channelViewModel.currentCategoryId.value == -1L) {
                                binding.channelNumber.visibility = View.VISIBLE
                                binding.channelName.visibility = View.INVISIBLE
                                binding.channelMediaInfo.visibility = View.GONE

                                jobFastChangeChannel = uiScope.launch {
                                    if (::jobFastChangeChannel.isInitialized && jobFastChangeChannel.isActive) jobFastChangeChannel.cancelAndJoin()
                                    println("newChannelIndex before: $channelIdFastSwitch")
                                    channelIdFastSwitch = channelViewModel.getNextChannelIndex(-1L, channelIdFastSwitch)
                                    println("newChannelIndex after: $channelIdFastSwitch")
                                    binding.channelNumber.text = (channelIdFastSwitch).toString()
                                }
                            }
                            else{
                                binding.channelNumber.visibility = View.VISIBLE
                                binding.channelName.visibility = View.INVISIBLE
                                binding.channelMediaInfo.visibility = View.GONE

                                jobFastChangeChannel = uiScope.launch {
                                    if (::jobFastChangeChannel.isInitialized && jobFastChangeChannel.isActive) jobFastChangeChannel.cancelAndJoin()
                                    println("newChannelIndex before: $channelIdFastSwitch")
                                    channelIdFastSwitch = channelViewModel.getNextChannelIndex(channelViewModel.currentCategoryId.value!!, channelIdFastSwitch)
                                    println("newChannelIndex after: $channelIdFastSwitch")
                                    binding.channelNumber.text = (channelIdFastSwitch).toString()
                                }
                            }
                        }
                        else{
                            if (!isLongPressDown){
                                if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                                    playerViewModel.hideChannelNumberKeyboard()
                                    if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive){
                                        jobUIChangeChannel.cancel()
                                    }
                                }
                                onLoadNextChannel()
                            }
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (binding.channelList.isVisible
                        || binding.rvChannelSettings.isVisible
                        || binding.rvCategoryList.isVisible) { // Navigate through menu
                        return false
                    } else if (binding.rvChannelTrackSettings.isVisible) {
                        return rvAudioTracks.adapter!!.itemCount == 0 || rvSubtitlesTracks.adapter!!.itemCount == 0
                    }
                    else if (binding.rvNumberList.isVisible) {
                        return true
                    }
                    else{ // Change to previous channel
                        if (event.repeatCount > 0) {
                            //if (isLongPressUp) return true // Uncomment to disallow 'channel fast switch direction' change
                            isLongPressDown = true
                            if (channelViewModel.currentCategoryId.value == -1L) {
                                binding.channelNumber.visibility = View.VISIBLE
                                binding.channelName.visibility = View.INVISIBLE
                                binding.channelMediaInfo.visibility = View.GONE

                                jobFastChangeChannel = uiScope.launch {
                                    println("newChannelIndex before: $channelIdFastSwitch")
                                    channelIdFastSwitch = channelViewModel.getPreviousChannelIndex(-1L, channelIdFastSwitch)
                                    println("newChannelIndex after: $channelIdFastSwitch")
                                    binding.channelNumber.text = (channelIdFastSwitch).toString()
                                }
                            }
                            else{
                                binding.channelNumber.visibility = View.VISIBLE
                                binding.channelName.visibility = View.INVISIBLE
                                binding.channelMediaInfo.visibility = View.GONE
                                jobFastChangeChannel = uiScope.launch {
                                    if (::jobFastChangeChannel.isInitialized && jobFastChangeChannel.isActive) jobFastChangeChannel.cancelAndJoin()
                                    println("newChannelIndex before: $channelIdFastSwitch")
                                    channelIdFastSwitch = channelViewModel.getPreviousChannelIndex(channelViewModel.currentCategoryId.value!!, channelIdFastSwitch)
                                    println("newChannelIndex after: $channelIdFastSwitch")
                                    binding.channelNumber.text = (channelIdFastSwitch).toString()
                                }
                            }

                        }
                        else{
                            if (!isLongPressUp){
                                if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                                    playerViewModel.hideChannelNumberKeyboard()
                                    if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive){
                                        jobUIChangeChannel.cancel()
                                    }
                                }
                                onLoadPreviousChannel()
                            }
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (binding.rvChannelSettings.isVisible) {
                        if (event.repeatCount > 0) return true
                        playerViewModel.hideSettingsMenu()
                        return true
                    }
                    else if (binding.channelList.isVisible
                        || binding.rvCategoryList.isVisible
                        || binding.rvChannelTrackSettings.isVisible) {
                        return true
                    }
                    else if (binding.rvNumberList.isVisible) {
                        return false
                    }
                    else{
                        if (event.repeatCount > 0) return true
                        onInitSettingsMenu()
                        playerViewModel.showSettingsMenu()
                        rvChannelSettings.requestFocus()
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (binding.rvChannelTrackSettings.isVisible
                        || binding.rvChannelSettings.isVisible
                        || binding.channelList.isVisible
                    ) {
                        return true
                    } else if (binding.rvNumberList.isVisible) {
                        return false
                    } else if (binding.rvCategoryList.visibility != View.VISIBLE) {
                        if (event.repeatCount > 0) return true
                        playerViewModel.showCategoryList()
                        rvCategoryList.requestFocus()
                        return true
                    }
                    else if (binding.rvCategoryList.isVisible) {
                        if (event.repeatCount > 0) return true
                        playerViewModel.hideCategoryList()
                    }
                    return true
                }

                (KeyEvent.KEYCODE_MENU) -> {
                    if (event.repeatCount > 0) return true
                    if (binding.channelList.isVisible) {
                        playerViewModel.hideChannelList()
                        return true
                    }
                    else if (playerViewModel.isSettingsMenuVisible.value == true) {
                        playerViewModel.hideSettingsMenu()
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true) {
                        playerViewModel.hideTrackMenu()
                    }
                    else if (playerViewModel.isCategoryListVisible.value == true) {
                        playerViewModel.hideCategoryList()
                    }
                    uiScope.launch {
                        onInitChannelList()
                        rvChannelList.scrollToPosition(playerViewModel.currentItemSelectedFromChannelList.value!!)
                        playerViewModel.showChannelList()
                        rvChannelList.requestFocus()
                    }
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (binding.channelList.isVisible) {
                        playerViewModel.hideChannelList()
                        return true
                    }
                    else if (playerViewModel.isSettingsMenuVisible.value == true) {
                        playerViewModel.hideSettingsMenu()
                        return true
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true) {
                        playerViewModel.hideTrackMenu()
                        return true
                    }
                    else if (playerViewModel.isCategoryListVisible.value == true) {
                        playerViewModel.hideCategoryList()
                        return true
                    }
                    else if (playerViewModel.isNumberListMenuVisible.value == true) {
                        playerViewModel.hideNumberListMenu()
                        return true
                    }
                    else if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                        playerViewModel.hideChannelNumberKeyboard()
                        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        return true
                    }
                    else if (playerViewModel.isChannelNumberVisible.value == true) {
                        playerViewModel.hideChannelNumber()
                        return true
                    }
                    else if (playerViewModel.isMediaInfoVisible.value == true) {
                        playerViewModel.hideMediaInfo()
                        return true
                    }
                    else{
                        activity?.finish()
                        return true
                    }
                }

                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                    if (isLongPressUp || isLongPressDown) return true
                    if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) {
                        jobUIChangeChannel.cancel()
                    }
                    if (binding.channelList.isVisible) return true
                    if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                    if (playerViewModel.isChannelNameVisible.value == true) playerViewModel.hideChannelName()
                    if (playerViewModel.isChannelNumberVisible.value == true) playerViewModel.hideChannelNumber()
                    if (playerViewModel.isCategoryNameVisible.value == true) playerViewModel.hideCategoryName()
                    if (playerViewModel.getCurrentNumberInput().length >= MAX_DIGITS) {
                        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                    }

                    val number = (code - KeyEvent.KEYCODE_0).toString()

                    playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().append(number))

                    binding.channelNumberKeyboard.text = playerViewModel.getCurrentNumberInput().toString()

                    onShowChannelNumberWithTimeoutAndChangeChannel()

                    return true
                }
            }
        }
        return false
    }
}
