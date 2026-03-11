package com.gaarx.tvplayer.ui.view

import android.content.Context
import com.gaarx.tvplayer.core.Constants.TIMEOUT_UI_CHANNEL_LOAD
import com.gaarx.tvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.tvplayer.ui.viewmodel.PlayerViewModel
import com.gaarx.tvplayer.util.DeviceUtil
import kotlinx.coroutines.*
import kotlin.reflect.KMutableProperty0

class PlayerUIController(
    private val context: Context,
    private val playerViewModel: PlayerViewModel,
    private val channelViewModel: ChannelViewModel,
    private val scope: CoroutineScope
) {
    var jobUITimeout: Job? = null
    var jobUIChangeChannel: Job? = null
    var jobEPGRender: Job? = null

    fun showStandardButtons() {
        if (!DeviceUtil.isAndroidTV(context)) {
            playerViewModel.apply {
                showButtonUp(); showButtonDown(); showButtonChannelList()
                showButtonSettings(); showButtonPiP(); showButtonCategoryList()
            }
        }
    }

    fun hideStandardButtons() {
        if (!DeviceUtil.isAndroidTV(context)) {
            playerViewModel.apply {
                hideButtonUp(); hideButtonDown(); hideButtonChannelList()
                hideButtonSettings(); hideButtonPiP(); hideButtonCategoryList()
            }
        }
    }

    fun showChannelInfo(includeTimeDate: Boolean = false) {
        playerViewModel.showChannelNumber()
        if (playerViewModel.currentCategoryId.value != -1L) playerViewModel.showCategoryName()
        playerViewModel.showChannelName()
        if (includeTimeDate) {
            playerViewModel.updateTimeDate()
            playerViewModel.showTimeDate()
        } else {
            playerViewModel.hideTimeDate()
        }
    }

    fun hideChannelInfo() {
        playerViewModel.apply {
            hideChannelNumber(); hideChannelName()
            hideCategoryName(); hideTimeDate()
        }
    }

    fun showMediaAndBottomInfo(alwaysBottom: Boolean = true) {
        if (playerViewModel.isSourceLoading.value == false) playerViewModel.showMediaInfo()
        if (alwaysBottom || channelViewModel.currentProgram.value != null || channelViewModel.nextProgram.value != null) {
            playerViewModel.showBottomInfo()
        }
    }

    fun hideMediaAndBottomInfo() {
        playerViewModel.hideMediaInfo()
        playerViewModel.hideBottomInfo()
    }

    fun launchUiWithTimeout(
        timeout: Long,
        epgDelay: Long = 0,
        updateEpg: Boolean = true,
        jobRef: KMutableProperty0<Job?> = ::jobUITimeout,
        extraAfterTimeout: (suspend () -> Unit)? = null,
        showUi: () -> Unit,
        hideUi: () -> Unit
    ) {
        jobRef.get()?.cancel()
        jobRef.set(null)

        if (updateEpg) {
            jobEPGRender?.cancel()
            jobEPGRender = scope.launch {
                playerViewModel.currentChannel.value?.let {
                    if (epgDelay > 0) delay(epgDelay)
                    channelViewModel.updateCurrentProgramForChannel(it.id)
                }
            }
        }

        jobRef.set(scope.launch {
            try {
                withContext(Dispatchers.Main) { showUi() }
                delay(timeout)
                ensureActive()
                extraAfterTimeout?.invoke()
                withContext(Dispatchers.Main) { hideUi() }
            } catch (_: CancellationException) { }
        })
    }

    fun showChannelInfoWithTimeout() {
        launchUiWithTimeout(
            timeout = TIMEOUT_UI_CHANNEL_LOAD,
            epgDelay = 0,
            showUi = {
                showStandardButtons()
                showChannelInfo(includeTimeDate = false)
                showMediaAndBottomInfo(alwaysBottom = true)
            },
            hideUi = {
                hideStandardButtons()
                hideChannelInfo()
                hideMediaAndBottomInfo()
            }
        )
    }

    fun showFullChannelUIWithTimeout(timeout: Long = 4000) {
        launchUiWithTimeout(
            timeout = timeout,
            epgDelay = 100,
            showUi = {
                showStandardButtons()
                showChannelInfo(includeTimeDate = true)
                showMediaAndBottomInfo(alwaysBottom = false)
            },
            hideUi = {
                hideStandardButtons()
                hideChannelInfo()
                hideMediaAndBottomInfo()
            }
        )
    }

    fun showChannelNumberWithTimeoutAndChangeChannel(onLoadChannel: (com.gaarx.tvplayer.domain.model.ChannelItem) -> Unit) {
        launchUiWithTimeout(
            timeout = 3000L,
            updateEpg = false,
            jobRef = ::jobUIChangeChannel,
            showUi = {
                playerViewModel.showChannelNumberKeyboard()
                playerViewModel.hideBottomInfo()
            },
            extraAfterTimeout = {
                try {
                    channelViewModel.updateIsLoadingChannel(true)
                    val newChannel = channelViewModel.getChannel(
                        categoryId = -1L,
                        playerViewModel.getCurrentNumberInput().toString().toInt()
                    )
                    withContext(Dispatchers.Main) { playerViewModel.hideChannelNumberKeyboard() }
                    if (newChannel.id != playerViewModel.currentChannel.value?.id) {
                        playerViewModel.updateCurrentCategoryId(-1L)
                        playerViewModel.updateCategoryName("Favoritos")
                        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        channelViewModel.updateLastCategoryLoaded(-1L)
                        channelViewModel.updateIsLoadingChannel(false)
                        withContext(Dispatchers.Main) { onLoadChannel(newChannel) }
                    } else {
                        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        channelViewModel.updateIsLoadingChannel(false)
                        withContext(Dispatchers.Main) { showChannelInfoWithTimeout() }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        playerViewModel.hideChannelNumberKeyboard()
                        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        channelViewModel.updateIsLoadingChannel(false)
                    }
                }
            },
            hideUi = { }
        )
    }
}
