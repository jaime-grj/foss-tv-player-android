package com.gaarx.tvplayer.ui.handler

import androidx.leanback.widget.VerticalGridView
import com.gaarx.tvplayer.domain.model.CategoryItem
import com.gaarx.tvplayer.domain.model.ChannelItem
import com.gaarx.tvplayer.ui.adapters.CategoryListAdapter
import com.gaarx.tvplayer.ui.adapters.ChannelListAdapter
import com.gaarx.tvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.tvplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChannelNavigationHandler(
    private val playerViewModel: PlayerViewModel,
    private val channelViewModel: ChannelViewModel,
    private val rvChannelList: VerticalGridView,
    private val rvCategoryList: VerticalGridView,
    private val uiScope: CoroutineScope,
    private val onLoadChannel: (ChannelItem) -> Unit,
    private val onInitChannelList: suspend () -> Unit,
    private val onShowChannelInfoWithTimeout: () -> Unit
) {
    fun handleDpadCenter(): Boolean {
        return when {
            playerViewModel.isChannelListVisible.value == true -> {
                handleChannelListSelection()
                true
            }
            playerViewModel.isCategoryListVisible.value == true -> {
                handleCategoryListSelection()
                true
            }
            else -> false
        }
    }

    fun handleDpadLeft(repeatCount: Int): Boolean {
        if (playerViewModel.isCategoryListVisible.value == true) {
            if (repeatCount == 0) playerViewModel.hideCategoryList()
            return true
        }

        // Only show category list if no other major menu is visible
        if (playerViewModel.isSettingsMenuVisible.value == true ||
            playerViewModel.isTrackMenuVisible.value == true ||
            playerViewModel.isChannelListVisible.value == true) {
            return true
        }

        if (repeatCount == 0) {
            playerViewModel.showCategoryList()
            rvCategoryList.requestFocus()
        }
        return true
    }

    fun handleMenu(repeatCount: Int): Boolean {
        if (repeatCount > 0) return true
        
        if (playerViewModel.isChannelListVisible.value == true) {
            playerViewModel.hideChannelList()
            return true
        }

        uiScope.launch {
            onInitChannelList()
            val position = playerViewModel.currentItemSelectedFromChannelList.value ?: 0
            rvChannelList.scrollToPosition(position)
            playerViewModel.showChannelList()
            rvChannelList.requestFocus()
        }
        return true
    }

    fun handleBack(): Boolean {
        return when {
            playerViewModel.isChannelListVisible.value == true -> {
                playerViewModel.hideChannelList()
                true
            }
            playerViewModel.isCategoryListVisible.value == true -> {
                playerViewModel.hideCategoryList()
                true
            }
            else -> false
        }
    }

    private fun handleChannelListSelection() {
        val focusedView = rvChannelList.focusedChild
        if (focusedView != null) {
            val position = rvChannelList.getChildAdapterPosition(focusedView)
            val newChannelSm = (rvChannelList.adapter as? ChannelListAdapter)?.getItemAtPosition(position) as? ChannelItem
            if (newChannelSm != null) {
                uiScope.launch {
                    val categoryId = playerViewModel.currentCategoryId.value ?: -1L
                    val index = if (categoryId == -1L) newChannelSm.indexFavourite!! else newChannelSm.indexGroup!!
                    val newChannel = channelViewModel.getNextChannel(categoryId, index)
                    onLoadChannel(newChannel)
                }
            }
        } else {
            channelViewModel.updateIsLoadingChannel(false)
            onShowChannelInfoWithTimeout()
        }
    }

    private fun handleCategoryListSelection() {
        val focusedView = rvCategoryList.focusedChild
        if (focusedView != null) {
            val position = rvCategoryList.getChildAdapterPosition(focusedView)
            val newCategory = (rvCategoryList.adapter as? CategoryListAdapter)?.getItemAtPosition(position) as? CategoryItem
            if (newCategory != null) {
                channelViewModel.updateIsLoadingChannel(true)
                playerViewModel.updateCurrentCategoryId(newCategory.id)
                playerViewModel.updateCategoryName(newCategory.name)
                uiScope.launch {
                    val firstChannel = channelViewModel.getNextChannel(categoryId = newCategory.id, groupId = 1)
                    onLoadChannel(firstChannel)
                    channelViewModel.updateIsLoadingChannel(false)
                    channelViewModel.updateLastCategoryLoaded(newCategory.id)
                }
                hideAllInfoOverlays()
                playerViewModel.hideCategoryList()
            }
        } else {
            channelViewModel.updateIsLoadingChannel(false)
            playerViewModel.hideCategoryList()
        }
    }

    private fun hideAllInfoOverlays() {
        playerViewModel.hideChannelName()
        playerViewModel.hideCategoryName()
        playerViewModel.hideChannelNumber()
        playerViewModel.hideBottomInfo()
        playerViewModel.hideMediaInfo()
        playerViewModel.hideTimeDate()
    }
}
