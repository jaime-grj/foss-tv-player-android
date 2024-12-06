package com.gaarj.iptvplayer.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaarj.iptvplayer.data.EPGRepository
import com.gaarj.iptvplayer.data.SettingsRepository
import com.gaarj.iptvplayer.domain.DownloadEPGUseCase
import com.gaarj.iptvplayer.domain.GetChannelsUseCase
import com.gaarj.iptvplayer.domain.GetSettingsUseCase
import com.gaarj.iptvplayer.domain.ImportJSONDataUseCase
import com.gaarj.iptvplayer.domain.UpdateEPGUseCase
import com.gaarj.iptvplayer.domain.GetCategoriesUseCase
import com.gaarj.iptvplayer.domain.model.CategoryItem
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.EPGProgramItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val updateEPGUseCase: UpdateEPGUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val importJSONDataUseCase: ImportJSONDataUseCase,
    private val downloadEPGUseCase: DownloadEPGUseCase,
    private val epgRepository: EPGRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase
): ViewModel() {

    private val _currentChannel = MutableLiveData<ChannelItem>()
    val currentChannel: LiveData<ChannelItem> get() = _currentChannel

    fun updateCurrentChannel(newChannel: ChannelItem) {
        _currentChannel.value = newChannel
    }

    private val _isInFavouriteCategory = MutableLiveData<Boolean>()
    val isInFavouriteCategory: LiveData<Boolean> get() = _isInFavouriteCategory

    fun updateIsInFavouriteCategory(isInFavouriteCategory: Boolean) {
        _isInFavouriteCategory.value = isInFavouriteCategory
    }

    private val _isLoadingChannelList = MutableLiveData<Boolean>()
    val isLoadingChannelList: LiveData<Boolean> get() = _isLoadingChannelList

    private val _isLoadingCategoryList = MutableLiveData<Boolean>()
    val isLoadingCategoryList: LiveData<Boolean> get() = _isLoadingCategoryList

    private val _channels = MutableLiveData<List<ChannelItem>>()
    val channels: LiveData<List<ChannelItem>> get() = _channels

    private val _categories = MutableLiveData<List<CategoryItem>>()
    val categories: LiveData<List<CategoryItem>> get() = _categories

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

    fun importJSONData() {
        viewModelScope.launch {
            updateIsLoadingChannelList(true)
            importJSONDataUseCase.invoke()
            getChannelsByCategory(-1L)
        }
    }

    fun downloadEPG(){
        viewModelScope.launch {
            Log.d("ChannelViewModel", "downloadEPG")
            val lastDownloadedTime = getSettingsUseCase.invoke()
            println("lastDownloadedTime: $lastDownloadedTime, current: ${System.currentTimeMillis() - lastDownloadedTime}")
            if (lastDownloadedTime <= 0L || System.currentTimeMillis() - lastDownloadedTime > 2 * 60 * 60 * 1000) {
                downloadEPGUseCase.invoke()
                settingsRepository.updateLastDownloadedTime(System.currentTimeMillis())
                updateEPGUseCase.invoke(channelList = _channels.value.orEmpty())
            }
            else{
                updateEPGUseCase.invoke(channelList = _channels.value.orEmpty())
            }
        }
    }

    fun updateIsLoadingChannelList(isLoading: Boolean) {
        _isLoadingChannelList.value = isLoading
    }

    fun getChannelsByCategory(categoryId: Long) {
        viewModelScope.launch {
            updateIsLoadingChannelList(true)
            _channels.value = getChannelsUseCase.invoke(categoryId)
            updateIsLoadingChannelList(false)
        }
    }

    suspend fun getEPGProgramsForChannel(channelId: Long): List<EPGProgramItem> {
        val programs : MutableList<EPGProgramItem> = mutableListOf()
        programs.addAll(epgRepository.getEPGProgramsForChannel(channelId))
        return programs
    }

    fun getSortedChannelsByIndexFavourite(): List<ChannelItem> {
        Log.d("ChannelViewModel", channels.value?.size.toString())
        return channels.value.orEmpty().sortedWith(compareBy(nullsLast()) { it.indexFavourite })
    }

    fun channelExists(id: Long): Boolean {
        return channels.value.orEmpty().any { it.id == id }
    }

    fun getChannelByFavouriteId(favId: Int): ChannelItem? {
        return channels.value.orEmpty().firstOrNull { it.indexFavourite == favId }
    }

    fun getChannelByGroupId(groupId: Int): ChannelItem? {
        return channels.value.orEmpty().firstOrNull { it.indexGroup == groupId }
    }

    fun updateCurrentProgramForChannel(id: Long) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val currentProgram = channels.value.orEmpty().first { it.id == id }.epgPrograms.firstOrNull{
                it.startTime.time <= currentTime && it.stopTime.time >= currentTime
            }
            val nextProgram = channels.value.orEmpty().first { it.id == id }.epgPrograms.firstOrNull{
                it.startTime.time > currentTime
            }
            updateCurrentProgram(currentProgram)
            updateNextProgram(nextProgram)
        }
    }

    fun updateEPG() {
        viewModelScope.launch {
            downloadEPGUseCase.invoke()
            updateEPGUseCase.invoke(channels.value.orEmpty())
        }
    }

    fun setCategories() {
        viewModelScope.launch {
            updateIsLoadingCategoryList(true)
            _categories.value = getCategoriesUseCase.invoke()
            updateIsLoadingCategoryList(false)
        }
    }

    fun updateIsLoadingCategoryList(isLoading: Boolean) {
        _isLoadingCategoryList.value = isLoading
    }
}