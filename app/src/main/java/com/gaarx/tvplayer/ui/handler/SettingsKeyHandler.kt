package com.gaarx.tvplayer.ui.handler

import androidx.annotation.OptIn
import androidx.recyclerview.widget.RecyclerView
import androidx.leanback.widget.VerticalGridView
import androidx.media3.common.util.UnstableApi
import com.gaarx.tvplayer.domain.model.*
import com.gaarx.tvplayer.ui.adapters.*
import com.gaarx.tvplayer.ui.view.PlayerManager
import com.gaarx.tvplayer.ui.viewmodel.PlayerViewModel

class SettingsKeyHandler @OptIn(UnstableApi::class) constructor
    (
    private val playerViewModel: PlayerViewModel,
    private val rvChannelSettings: RecyclerView,
    private val rvAudioTracks: VerticalGridView,
    private val rvSubtitlesTracks: VerticalGridView,
    private val rvChannelSources: VerticalGridView,
    private val rvVideoTracks: VerticalGridView,
    private val playerManager: PlayerManager,
    private val onLoadSetting: (Int) -> Unit,
    private val onLoadStreamSource: (StreamSourceItem) -> Unit,
    private val onInitSettingsMenu: () -> Unit
) {
    fun handleDpadCenter(): Boolean {
        return when {
            playerViewModel.isSettingsMenuVisible.value == true -> {
                handleSettingsMenuSelection()
                true
            }
            playerViewModel.isTrackMenuVisible.value == true -> {
                handleTrackMenuSelection()
                true
            }
            else -> false
        }
    }

    fun handleDpadRight(repeatCount: Int): Boolean {
        if (playerViewModel.isSettingsMenuVisible.value == true) {
            if (repeatCount > 0) return true
            playerViewModel.hideSettingsMenu()
            return true
        }

        if (playerViewModel.isChannelListVisible.value == true
            || playerViewModel.isCategoryListVisible.value == true
            || playerViewModel.isTrackMenuVisible.value == true) {
            return true
        }

        if (repeatCount > 0) return true
        onInitSettingsMenu()
        playerViewModel.showSettingsMenu()
        rvChannelSettings.requestFocus()
        return true
    }

    fun handleBack(): Boolean {
        return when {
            playerViewModel.isSettingsMenuVisible.value == true -> {
                playerViewModel.hideSettingsMenu()
                true
            }
            playerViewModel.isTrackMenuVisible.value == true -> {
                playerViewModel.hideTrackMenu()
                true
            }
            else -> false
        }
    }

    fun isSettingsOrTrackMenuVisible(): Boolean {
        return playerViewModel.isSettingsMenuVisible.value == true ||
                playerViewModel.isTrackMenuVisible.value == true
    }

    private fun handleSettingsMenuSelection() {
        val focusedView = rvChannelSettings.focusedChild
        if (focusedView != null) {
            val position = rvChannelSettings.getChildAdapterPosition(focusedView)
            playerViewModel.hideSettingsMenu()
            onLoadSetting(position)
        } else {
            playerViewModel.hideSettingsMenu()
        }
    }

    private fun handleTrackMenuSelection() {
        when (playerViewModel.currentLoadedMenuSetting.value) {
            ChannelSettings.AUDIO_TRACKS -> handleAudioTrackSelection()
            ChannelSettings.SUBTITLES_TRACKS -> handleSubtitleTrackSelection()
            ChannelSettings.SOURCES -> handleSourceSelection()
            ChannelSettings.VIDEO_TRACKS -> handleVideoTrackSelection()
        }
    }

    @OptIn(UnstableApi::class)
    private fun handleAudioTrackSelection() {
        val focusedView = rvAudioTracks.focusedChild
        if (focusedView != null) {
            val position = rvAudioTracks.getChildAdapterPosition(focusedView)
            (rvAudioTracks.adapter as? AudioTracksAdapter)?.getItemAtPosition(position)?.let {
                playerManager.loadAudioTrack(it as AudioTrack)
            }
        }
        playerViewModel.hideTrackMenu()
    }

    @OptIn(UnstableApi::class)
    private fun handleSubtitleTrackSelection() {
        val focusedView = rvSubtitlesTracks.focusedChild
        if (focusedView != null) {
            val position = rvSubtitlesTracks.getChildAdapterPosition(focusedView)
            (rvSubtitlesTracks.adapter as? SubtitlesTracksAdapter)?.getItemAtPosition(position)?.let {
                playerManager.loadSubtitlesTrack(it as SubtitlesTrack)
            }
        }
        playerViewModel.hideTrackMenu()
    }

    private fun handleSourceSelection() {
        val focusedView = rvChannelSources.focusedChild
        if (focusedView != null) {
            val position = rvChannelSources.getChildAdapterPosition(focusedView)
            if (playerViewModel.isErrorMessageVisible.value == true) {
                playerViewModel.hideErrorMessage()
            }
            playerViewModel.hideBottomErrorMessage()
            var streamSource = (rvChannelSources.adapter as? ChannelSourcesAdapter)?.getItemAtPosition(position) as StreamSourceItem
            if (streamSource.id.toInt() == -1) {
                playerViewModel.updateIsSourceForced(false)
                playerViewModel.currentChannel.value?.streamSources?.minByOrNull { it.index }?.let {
                    streamSource = it
                }
            } else {
                playerViewModel.updateIsSourceForced(true)
            }
            playerViewModel.updateTriesCountForEachSource(0)
            playerViewModel.updateSourcesTriedCount(0)
            playerViewModel.hideAnimatedLoadingIcon()
            onLoadStreamSource(streamSource)
            playerViewModel.hidePlayer()
        }
        playerViewModel.hideTrackMenu()
    }

    @OptIn(UnstableApi::class)
    private fun handleVideoTrackSelection() {
        // Keeping rvChannelSources as it was in original KeyEventHandler, likely a shared view for selection logic
        val focusedView = rvChannelSources.focusedChild
        if (focusedView != null) {
            val position = rvChannelSources.getChildAdapterPosition(focusedView)
            (rvVideoTracks.adapter as? VideoTracksAdapter)?.getItemAtPosition(position)?.let { track ->
                val videoTrack = track as VideoTrack
                try {
                    playerViewModel.updateIsQualityForced(videoTrack.id.toInt() != -1)
                } catch (_: Exception) {}
                playerManager.loadVideoTrack(videoTrack)
            }
        }
        playerViewModel.hideTrackMenu()
    }
}
