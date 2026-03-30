package com.gaarx.tvplayer.ui.handler

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import com.gaarx.tvplayer.core.Constants.TIMEOUT_UI_INFO
import com.gaarx.tvplayer.databinding.FragmentPlayerBinding
import com.gaarx.tvplayer.domain.model.*
import com.gaarx.tvplayer.ui.view.PlayerManager
import com.gaarx.tvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.tvplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

@OptIn(UnstableApi::class)
class KeyEventHandler(
    private val binding: FragmentPlayerBinding,
    private val playerViewModel: PlayerViewModel,
    private val channelViewModel: ChannelViewModel,
    private val playerManager: PlayerManager,
    private val uiScope: CoroutineScope,
    private val activity: FragmentActivity,
    private val actions: KeyHandlerActions
) {
    private var jobUIChangeChannel: Job? = null

    private val fastSwitchHandler = FastSwitchHandler(
        playerViewModel = playerViewModel,
        channelViewModel = channelViewModel,
        binding = binding,
        uiScope = uiScope,
        onLoadChannel = actions.onLoadChannel
    )

    private val settingsKeyHandler = SettingsKeyHandler(
        playerViewModel = playerViewModel,
        rvChannelSettings = binding.rvChannelSettings,
        rvAudioTracks = binding.rvChannelTrackSettings,
        rvSubtitlesTracks = binding.rvChannelTrackSettings,
        rvChannelSources = binding.rvChannelTrackSettings,
        rvVideoTracks = binding.rvChannelTrackSettings,
        playerManager = playerManager,
        onLoadSetting = actions.onLoadSetting,
        onLoadStreamSource = actions.onLoadStreamSource,
        onInitSettingsMenu = actions.onInitSettingsMenu
    )

    private val channelNavigationHandler = ChannelNavigationHandler(
        playerViewModel = playerViewModel,
        channelViewModel = channelViewModel,
        rvChannelList = binding.channelList,
        rvCategoryList = binding.rvCategoryList,
        uiScope = uiScope,
        onLoadChannel = actions.onLoadChannel,
        onInitChannelList = actions.onInitChannelList,
        onShowChannelInfoWithTimeout = actions.onShowChannelInfoWithTimeout
    )

    private val numberInputHandler = NumberInputHandler(
        playerViewModel = playerViewModel,
        channelViewModel = channelViewModel,
        binding = binding,
        rvNumberList = binding.rvNumberList,
        uiScope = uiScope,
        onLoadChannel = actions.onLoadChannel,
        onShowChannelInfoWithTimeout = actions.onShowChannelInfoWithTimeout,
        onShowChannelNumberWithTimeoutAndChangeChannel = actions.onShowChannelNumberWithTimeoutAndChangeChannel,
        cancelUIChangeJob = { jobUIChangeChannel?.cancel() }
    )

    fun handle(event: KeyEvent): Boolean {
        if (channelViewModel.isLoadingChannelList.value == true
            || channelViewModel.isImportingData.value == true
            || channelViewModel.isLoadingChannel.value == true) return true
            
        val code = event.keyCode
        val action = event.action
        val isDown = action == KeyEvent.ACTION_DOWN

        if (!isDown) {
            return fastSwitchHandler.handleKeyUp()
        } else {
            when (code) {
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    if (fastSwitchHandler.isLongPressActive()) return true

                    if (settingsKeyHandler.handleDpadCenter()) return true
                    if (channelNavigationHandler.handleDpadCenter()) return true
                    if (numberInputHandler.handleDpadCenter(event)) return true

                    if (event.repeatCount == 0) {
                        jobUIChangeChannel?.cancel()
                        if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                            playerViewModel.hideChannelNumberKeyboard()
                            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        }
                        actions.onShowFullChannelUIWithTimeout(TIMEOUT_UI_INFO)
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (isMenuVisible()) return false
                    
                    if (binding.rvChannelTrackSettings.isVisible) {
                        return binding.rvChannelTrackSettings.adapter?.itemCount == 0
                    }
                    
                    if (binding.rvNumberList.isVisible) return true

                    if (fastSwitchHandler.handleDpadUp(event.repeatCount)) return true

                    if (!fastSwitchHandler.isLongPressActive()) {
                        if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                            playerViewModel.hideChannelNumberKeyboard()
                            jobUIChangeChannel?.cancel()
                        }
                        actions.onLoadNextChannel()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (isMenuVisible()) return false
                    
                    if (binding.rvChannelTrackSettings.isVisible) {
                        return binding.rvChannelTrackSettings.adapter?.itemCount == 0
                    }
                    
                    if (binding.rvNumberList.isVisible) return true

                    if (fastSwitchHandler.handleDpadDown(event.repeatCount)) return true

                    if (!fastSwitchHandler.isLongPressActive()) {
                        if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                            playerViewModel.hideChannelNumberKeyboard()
                            jobUIChangeChannel?.cancel()
                        }
                        actions.onLoadPreviousChannel()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (settingsKeyHandler.handleDpadRight(event.repeatCount)) return true
                    
                    if (isMenuVisible() || binding.rvChannelTrackSettings.isVisible) return true
                    
                    return binding.rvNumberList.isVisible
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (channelNavigationHandler.handleDpadLeft(event.repeatCount)) return true

                    if (binding.rvChannelTrackSettings.isVisible
                        || binding.rvChannelSettings.isVisible
                        || binding.channelList.isVisible
                    ) return true
                    
                    return binding.rvNumberList.isVisible
                }

                KeyEvent.KEYCODE_MENU -> {
                    if (channelNavigationHandler.handleMenu(event.repeatCount)) return true
                    
                    playerViewModel.hideSettingsMenu()
                    playerViewModel.hideTrackMenu()
                    playerViewModel.hideCategoryList()
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (settingsKeyHandler.handleBack()) return true
                    if (channelNavigationHandler.handleBack()) return true
                    if (numberInputHandler.handleBack()) return true

                    activity.finish()
                    return true
                }

                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                    if (fastSwitchHandler.isLongPressActive()) return true
                    if (binding.channelList.isVisible) return true
                    return numberInputHandler.handleNumberKey(code)
                }
            }
        }
        return false
    }

    private fun isMenuVisible(): Boolean {
        return binding.channelList.isVisible || binding.rvChannelSettings.isVisible || binding.rvCategoryList.isVisible
    }
}

data class KeyHandlerActions(
    val onLoadChannel: (ChannelItem) -> Unit,
    val onLoadNextChannel: () -> Unit,
    val onLoadPreviousChannel: () -> Unit,
    val onLoadStreamSource: (StreamSourceItem) -> Unit,
    val onShowChannelInfoWithTimeout: () -> Unit,
    val onShowFullChannelUIWithTimeout: (Long) -> Unit,
    val onShowChannelNumberWithTimeoutAndChangeChannel: () -> Unit,
    val onLoadSetting: (Int) -> Unit,
    val onInitSettingsMenu: () -> Unit,
    val onInitChannelList: suspend () -> Unit
)
