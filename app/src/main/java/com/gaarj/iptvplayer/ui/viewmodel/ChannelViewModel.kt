package com.gaarj.iptvplayer.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaarj.iptvplayer.domain.GetChannelsUseCase
import com.gaarj.iptvplayer.domain.GetCurrentProgramUseCase
import com.gaarj.iptvplayer.domain.GetNextProgramUseCase
import com.gaarj.iptvplayer.domain.UpdateEPGUseCase
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.EPGProgramItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val updateEPGUseCase: UpdateEPGUseCase,
    private val getCurrentProgramUseCase: GetCurrentProgramUseCase,
    private val getNextProgramUseCase: GetNextProgramUseCase
): ViewModel() {

    private val _currentChannel = MutableLiveData<ChannelItem>()
    val currentChannel: LiveData<ChannelItem> get() = _currentChannel


    fun updateCurrentChannel(newChannel: ChannelItem) {
        _currentChannel.value = newChannel
    }

    private val _channels = MutableLiveData<List<ChannelItem>>()
    val channels: LiveData<List<ChannelItem>> get() = _channels

    private val _currentProgram = MutableLiveData<EPGProgramItem?>()
    val currentProgram: LiveData<EPGProgramItem?> get() = _currentProgram

    private val _nextProgram = MutableLiveData<EPGProgramItem?>()
    val nextProgram: LiveData<EPGProgramItem?> get() = _nextProgram

    fun updateCurrentProgram(currentProgram: EPGProgramItem?) {
        _currentProgram.value = currentProgram
    }

    fun updateNextProgram(nextProgram: EPGProgramItem?) {
        _nextProgram.value = nextProgram
    }

    fun onCreate() {
        viewModelScope.launch {
            val result = getChannelsUseCase.invoke()
            if (result.isNotEmpty() && _channels.value.isNullOrEmpty()) _channels.value = result
            updateEPGUseCase()
        }
    }

    fun getSortedChannelsByIndexFavourite(): List<ChannelItem> {
        return channels.value.orEmpty().sortedWith(compareBy(nullsLast()) { it.indexFavourite })
    }

    fun channelExists(id: Long): Boolean {
        return channels.value.orEmpty().any { it.id == id }
    }

    fun getChannelByFavouriteId(favId: Int): ChannelItem? {
        return channels.value.orEmpty().firstOrNull { it.indexFavourite == favId }
    }

    fun getCurrentProgramForChannel(id: Long) {
        viewModelScope.launch {
            val currentProgram = getCurrentProgramUseCase.invoke(id)
            val nextProgram = getNextProgramUseCase.invoke(id)
            updateCurrentProgram(currentProgram)
            updateNextProgram(nextProgram)
        }
    }
}