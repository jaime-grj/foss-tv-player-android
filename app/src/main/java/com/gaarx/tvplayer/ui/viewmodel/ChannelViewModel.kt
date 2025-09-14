package com.gaarx.tvplayer.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaarx.tvplayer.data.ChannelRepository
import com.gaarx.tvplayer.data.EPGRepository
import com.gaarx.tvplayer.data.SettingsRepository
import com.gaarx.tvplayer.domain.DownloadEPGUseCase
import com.gaarx.tvplayer.domain.GetChannelsUseCase
import com.gaarx.tvplayer.domain.GetConfigURLUseCase
import com.gaarx.tvplayer.domain.GetSettingsUseCase
import com.gaarx.tvplayer.domain.ImportJSONDataUseCase
import com.gaarx.tvplayer.domain.UpdateConfigURLUseCase
import com.gaarx.tvplayer.domain.model.CategoryItem
import com.gaarx.tvplayer.domain.model.ChannelItem
import com.gaarx.tvplayer.domain.model.EPGProgramItem
import com.gaarx.tvplayer.domain.model.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val importJSONDataUseCase: ImportJSONDataUseCase,
    private val downloadEPGUseCase: DownloadEPGUseCase,
    private val getConfigURLUseCase: GetConfigURLUseCase,
    private val updateConfigURLUseCase: UpdateConfigURLUseCase,
    private val epgRepository: EPGRepository,
    private val channelRepository: ChannelRepository
): ViewModel() {

     suspend fun getChannelsByCategory(categoryId: Long): List<ChannelItem> {
        val channels = channelRepository.getChannelsByCategory(categoryId)
        return channels
     }

    suspend fun getSmChannelsByCategory(categoryId: Long): List<ChannelItem> {
        val channels = channelRepository.getSmChannelsByCategory(categoryId)
        return channels
    }

    suspend fun getChannelCountByCategory(categoryId: Long): Int {
        return channelRepository.getChannelCountByCategory(categoryId)
    }

    private val _isLoadingChannelList = MutableLiveData<Boolean>()
    val isLoadingChannelList: LiveData<Boolean> get() = _isLoadingChannelList

    private val _isLoadingChannel = MutableLiveData<Boolean>()
    val isLoadingChannel: LiveData<Boolean> get() = _isLoadingChannel

    fun updateIsLoadingChannel(isLoadingChannel: Boolean) {
        _isLoadingChannel.value = isLoadingChannel
    }

    private val _isImportingData = MutableLiveData<Boolean>()
    val isImportingData: LiveData<Boolean> get() = _isImportingData

    fun updateIsImportingData(isImportingData: Boolean) {
        _isImportingData.value = isImportingData
    }

    private val _categoriesWithChannels = MutableLiveData<List<CategoryItem>>()
    val categoriesWithChannels: LiveData<List<CategoryItem>> get() = _categoriesWithChannels

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

    suspend fun importJSONData() {
        updateIsImportingData(true)
        try {
            importJSONDataUseCase.invoke()
        } catch (e: Exception) {
            Log.e("ChannelViewModel", "Error importing JSON data", e)
        }
        updateIsImportingData(false)
    }

    suspend fun downloadEPG(){
        Log.d("ChannelViewModel", "downloadEPG")
        val lastDownloadedTime = getSettingsUseCase.invoke()
        println("lastDownloadedTime: $lastDownloadedTime, current: ${System.currentTimeMillis() - lastDownloadedTime}")
        if (lastDownloadedTime <= 0L || System.currentTimeMillis() - lastDownloadedTime > 2 * 60 * 60 * 1000) {
            downloadEPGUseCase.invoke()
            settingsRepository.updateLastDownloadedTime(System.currentTimeMillis())
        }
    }

    suspend fun getSmChannelsWithSchedule(): List<ChannelItem> {
        return channelRepository.getSmChannelsWithSchedule()
    }

    fun updateIsLoadingChannelList(isLoading: Boolean) {
        Log.d("ChannelViewModel", "isLoading: $isLoading")
        _isLoadingChannelList.value = isLoading
    }

    suspend fun getEPGProgramsForChannel(channelId: Long): List<EPGProgramItem> {
        val programs : MutableList<EPGProgramItem> = mutableListOf()
        programs.addAll(epgRepository.getEPGProgramsForChannel(channelId))
        return programs
    }

    suspend fun getPreviousChannel(categoryId: Long, groupId: Int): ChannelItem {
        return channelRepository.getPreviousChannel(categoryId, groupId)
    }

    suspend fun getNextChannel(categoryId: Long, groupId: Int): ChannelItem {
        return channelRepository.getNextChannel(categoryId, groupId)
    }

    suspend fun getNextChannelIndex(categoryId: Long, groupId: Int): Int {
        return channelRepository.getNextChannelIndex(categoryId, groupId)
    }

    suspend fun getPreviousChannelIndex(categoryId: Long, groupId: Int): Int {
        return channelRepository.getPreviousChannelIndex(categoryId, groupId)
    }

    suspend fun getChannel(categoryId: Long, groupId: Int): ChannelItem {
        return channelRepository.getChannel(categoryId, groupId)
    }

    suspend fun updateCurrentProgramForChannel(id: Long) {
        val currentProgram = epgRepository.getCurrentProgramForChannel(id)?.toDomain()
        val nextProgram = epgRepository.getNextProgramForChannel(id)?.toDomain()
        updateCurrentProgram(currentProgram)
        updateNextProgram(nextProgram)
    }

    fun updateEPG() {
        viewModelScope.launch {
            downloadEPGUseCase.invoke()
        }
    }

    suspend fun updateLastChannelLoaded(channelId: Long) {
        settingsRepository.updateLastChannelLoaded(channelId)
    }

    suspend fun getLastChannelLoaded(): Long {
        return settingsRepository.getLastChannelLoaded()
    }

    suspend fun updateLastCategoryLoaded(categoryId: Long) {
        settingsRepository.updateLastCategoryLoaded(categoryId)
    }

    suspend fun getLastCategoryLoaded(): Long {
        return settingsRepository.getLastCategoryLoaded()
    }

    suspend fun getCategories() : List<CategoryItem> {
        return channelRepository.getCategories()
    }

    suspend fun getChannelById(id: Long): ChannelItem? {
        return channelRepository.getChannelById(id)
    }

    suspend fun getCategoryById(id: Long): CategoryItem? {
        return channelRepository.getCategoryById(id)
    }

    suspend fun getChannelCount(): Int {
        return channelRepository.getChannelCount()
    }

    suspend fun updateConfigURL(url: String) {
        updateConfigURLUseCase.invoke(url)
    }

    suspend fun getConfigURL(): String {
        return getConfigURLUseCase.invoke()
    }

}