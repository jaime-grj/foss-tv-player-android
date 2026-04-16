package com.gaarx.tvplayer.ui.handler

import android.view.KeyEvent
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.tvplayer.core.Constants.MAX_DIGITS
import com.gaarx.tvplayer.databinding.FragmentPlayerBinding
import com.gaarx.tvplayer.domain.model.ChannelItem
import com.gaarx.tvplayer.exceptions.ChannelNotFoundException
import com.gaarx.tvplayer.ui.adapters.NumberListAdapter
import com.gaarx.tvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.tvplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NumberInputHandler(
    private val playerViewModel: PlayerViewModel,
    private val channelViewModel: ChannelViewModel,
    private val binding: FragmentPlayerBinding,
    private val rvNumberList: RecyclerView,
    private val uiScope: CoroutineScope,
    private val onLoadChannel: (ChannelItem) -> Unit,
    private val onShowChannelInfoWithTimeout: () -> Unit,
    private val onShowChannelNumberWithTimeoutAndChangeChannel: () -> Unit,
    private val cancelUIChangeJob: () -> Unit
) {
    fun handleNumberKey(code: Int): Boolean {
        cancelUIChangeJob()
        
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

    fun handleDpadCenter(event: KeyEvent): Boolean {
        return when {
            playerViewModel.isNumberListMenuVisible.value == true -> {
                handleNumberListSelection(event)
                true
            }
            playerViewModel.isChannelNumberKeyboardVisible.value == true -> {
                handleChannelKeyboardSelection()
                true
            }
            playerViewModel.isChannelNumberVisible.value == true -> {
                handleChannelNumberVisible(event)
                true
            }
            else -> false
        }
    }

    fun handleBack(): Boolean {
        return when {
            playerViewModel.isNumberListMenuVisible.value == true -> {
                playerViewModel.hideNumberListMenu()
                true
            }
            playerViewModel.isChannelNumberKeyboardVisible.value == true -> {
                playerViewModel.hideChannelNumberKeyboard()
                playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                true
            }
            else -> false
        }
    }

    fun handleDpadRight(): Boolean {
        if (playerViewModel.isNumberListMenuVisible.value != true) return false

        val focusedView = rvNumberList.focusedChild ?: return false
        val position = rvNumberList.getChildAdapterPosition(focusedView)
        val itemCount = rvNumberList.adapter?.itemCount ?: 0

        if (position == itemCount - 1) {
            rvNumberList.smoothScrollToPosition(0)
            rvNumberList.post {
                rvNumberList.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
            }
            return true
        }
        return false
    }

    fun handleDpadLeft(): Boolean {
        if (playerViewModel.isNumberListMenuVisible.value != true) return false

        val focusedView = rvNumberList.focusedChild ?: return false
        val position = rvNumberList.getChildAdapterPosition(focusedView)
        val itemCount = rvNumberList.adapter?.itemCount ?: 0

        if (position == 0 && itemCount > 0) {
            rvNumberList.smoothScrollToPosition(itemCount - 1)
            rvNumberList.post {
                rvNumberList.findViewHolderForAdapterPosition(itemCount - 1)?.itemView?.requestFocus()
            }
            return true
        }
        return false
    }

    private fun handleNumberListSelection(event: KeyEvent) {
        if (event.repeatCount > 0) return
        
        cancelUIChangeJob()
        
        hideOverlaysForNumberInput()
        
        if (playerViewModel.getCurrentNumberInput().length >= MAX_DIGITS) {
            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
        }

        val focusedView = rvNumberList.focusedChild
        if (focusedView != null) {
            val position = rvNumberList.getChildAdapterPosition(focusedView)
            val selectedNumber = (rvNumberList.adapter as? NumberListAdapter)?.getItemAtPosition(position) as? Int ?: return
            
            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().append(selectedNumber.toString()))
            binding.channelNumberKeyboard.text = playerViewModel.getCurrentNumberInput().toString()
            onShowChannelNumberWithTimeoutAndChangeChannel()
        } else {
            playerViewModel.hideNumberListMenu()
        }
    }

    private fun handleChannelKeyboardSelection() {
        channelViewModel.updateIsLoadingChannel(true)
        cancelUIChangeJob()
        
        val channelIndexStr = playerViewModel.getCurrentNumberInput().toString()
        val channelIndex = channelIndexStr.toIntOrNull() ?: return
        
        playerViewModel.hideChannelNumberKeyboard()
        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
        
        uiScope.launch {
            try {
                channelViewModel.updateIsLoadingChannel(true)
                val newChannel = channelViewModel.getChannel(-1L, channelIndex)
                playerViewModel.updateCurrentCategoryId(-1L)
                playerViewModel.updateCategoryName("Favoritos")
                channelViewModel.updateLastCategoryLoaded(-1L)
                
                if (playerViewModel.currentChannel.value?.id != newChannel.id) {
                    onLoadChannel(newChannel)
                } else {
                    onShowChannelInfoWithTimeout()
                }
            } catch (_: ChannelNotFoundException) {
                // Handled in original code by hiding keyboard and clearing input
            } finally {
                playerViewModel.hideChannelNumberKeyboard()
                playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                channelViewModel.updateIsLoadingChannel(false)
            }
        }
    }

    private fun handleChannelNumberVisible(event: KeyEvent): Boolean {
        if (event.repeatCount > 0) {
            if (playerViewModel.isNumberListMenuVisible.value != true) {
                hideOverlaysForNumberInput()
                playerViewModel.showNumberListMenu()
                rvNumberList.requestFocus()
            }
        } else {
            playerViewModel.hideChannelNumber()
            playerViewModel.hideChannelName()
            playerViewModel.hideCategoryName()
            playerViewModel.hideTimeDate()
            playerViewModel.hideMediaInfo()
            playerViewModel.hideBottomInfo()
        }
        return true
    }

    private fun hideOverlaysForNumberInput() {
        playerViewModel.hideMediaInfo()
        playerViewModel.hideChannelName()
        playerViewModel.hideChannelNumber()
        playerViewModel.hideCategoryName()
        playerViewModel.hideTimeDate()
        playerViewModel.hideBottomInfo()
    }
}
