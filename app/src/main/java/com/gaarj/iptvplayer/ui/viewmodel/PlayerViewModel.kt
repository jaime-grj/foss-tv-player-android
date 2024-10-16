package com.gaarj.iptvplayer.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gaarj.iptvplayer.domain.model.MediaInfo
import com.gaarj.iptvplayer.domain.model.StreamSourceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(): ViewModel() {

    private val _currentStreamSource = MutableLiveData<StreamSourceItem>()
    val currentStreamSource: LiveData<StreamSourceItem> get() = _currentStreamSource

    private val _isSourceForced = MutableLiveData<Boolean>()
    val isSourceForced: LiveData<Boolean> get() = _isSourceForced

    private val _sourcesTriedCount = MutableLiveData<Int>()
    val sourcesTriedCount: LiveData<Int> get() = _sourcesTriedCount

    private val _triesCountForEachSource = MutableLiveData<Int>()
    val triesCountForEachSource: LiveData<Int> get() = _triesCountForEachSource

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _isBuffering = MutableLiveData<Boolean>()
    val isBuffering: LiveData<Boolean> get() = _isBuffering

    private val _currentItemSelectedFromChannelList: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromChannelList: LiveData<Int> get() = _currentItemSelectedFromChannelList

    private val _currentItemSelectedFromChannelSettingsMenu: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromChannelSettingsMenu: LiveData<Int> get() = _currentItemSelectedFromChannelSettingsMenu

    private val _currentItemSelectedFromAudioTracksMenu: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromAudioTracksMenu: LiveData<Int> get() = _currentItemSelectedFromAudioTracksMenu

    private val _currentItemSelectedFromSubtitlesTracksMenu: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromSubtitlesTracksMenu: LiveData<Int> get() = _currentItemSelectedFromSubtitlesTracksMenu

    private val _currentItemSelectedFromChannelSourcesMenu: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromChannelSourcesMenu: LiveData<Int> get() = _currentItemSelectedFromChannelSourcesMenu

    private val _currentItemSelectedFromVideoTracksMenu: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromVideoTracksMenu: LiveData<Int> get() = _currentItemSelectedFromVideoTracksMenu

    private val _currentLoadedMenuSetting: MutableLiveData<Int> = MutableLiveData()
    val currentLoadedMenuSetting: LiveData<Int> get() = _currentLoadedMenuSetting

    private val _currentChannelSourcesList = MutableLiveData<List<StreamSourceItem>>()
    val currentChannelSourcesList: LiveData<List<StreamSourceItem>> get() = _currentChannelSourcesList

    fun onCreate() {
        _isSourceForced.value = false
        _sourcesTriedCount.value = 0
        _triesCountForEachSource.value = 0
        _isLoading.value = false
        _isBuffering.value = false
        _currentItemSelectedFromChannelList.value = -1
        _currentItemSelectedFromChannelSettingsMenu.value = 0
        _currentItemSelectedFromAudioTracksMenu.value = -1
        _currentItemSelectedFromSubtitlesTracksMenu.value = -1
        _currentItemSelectedFromChannelSourcesMenu.value = -1
        _currentItemSelectedFromVideoTracksMenu.value = -1
        _currentLoadedMenuSetting.value = -1
    }

    private val _mediaInfo = MutableLiveData<MediaInfo>()
    val mediaInfo: LiveData<MediaInfo> get() = _mediaInfo

    private val _channelNumber = MutableLiveData<Int>()
    val channelNumber: LiveData<Int> get() = _channelNumber

    private val _channelName = MutableLiveData<String>()
    val channelName: LiveData<String> get() = _channelName

    private val _timeDate = MutableLiveData<String>()
    val timeDate: LiveData<String> get() = _timeDate

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _isBottomInfoVisible = MutableLiveData<Boolean>()
    val isBottomInfoVisible: LiveData<Boolean> get() = _isBottomInfoVisible

    private val _isChannelListVisible = MutableLiveData<Boolean>()
    val isChannelListVisible: LiveData<Boolean> get() = _isChannelListVisible

    private val _isSettingsMenuVisible = MutableLiveData<Boolean>()
    val isSettingsMenuVisible: LiveData<Boolean> get() = _isSettingsMenuVisible

    private val _isTrackMenuVisible = MutableLiveData<Boolean>()
    val isTrackMenuVisible: LiveData<Boolean> get() = _isTrackMenuVisible

    private val _isMediaInfoVisible = MutableLiveData<Boolean>()
    val isMediaInfoVisible: LiveData<Boolean> get() = _isMediaInfoVisible

    private val _isChannelNumberVisible = MutableLiveData<Boolean>()
    val isChannelNumberVisible: LiveData<Boolean> get() = _isChannelNumberVisible

    private val _isChannelNameVisible = MutableLiveData<Boolean>()
    val isChannelNameVisible: LiveData<Boolean> get() = _isChannelNameVisible

    private val _isTimeDateVisible = MutableLiveData<Boolean>()
    val isTimeDateVisible: LiveData<Boolean> get() = _isTimeDateVisible

    private val _isErrorMessageVisible = MutableLiveData<Boolean>()
    val isErrorMessageVisible: LiveData<Boolean> get() = _isErrorMessageVisible

    private val _isPlayerVisible = MutableLiveData<Boolean>()
    val isPlayerVisible: LiveData<Boolean> get() = _isPlayerVisible

    private val _isButtonUpVisible = MutableLiveData<Boolean>()
    val isButtonUpVisible: LiveData<Boolean> get() = _isButtonUpVisible

    private val _isButtonDownVisible = MutableLiveData<Boolean>()
    val isButtonDownVisible: LiveData<Boolean> get() = _isButtonDownVisible

    private val _isButtonSettingsVisible = MutableLiveData<Boolean>()
    val isButtonSettingsVisible: LiveData<Boolean> get() = _isButtonSettingsVisible

    private val _isButtonChannelListVisible = MutableLiveData<Boolean>()
    val isButtonChannelListVisible: LiveData<Boolean> get() = _isButtonChannelListVisible

    private val _isButtonPiPVisible = MutableLiveData<Boolean>()
    val isButtonPiPVisible: LiveData<Boolean> get() = _isButtonPiPVisible

    private val _isChannelNumberKeyboardVisible = MutableLiveData<Boolean>()
    val isChannelNumberKeyboardVisible: LiveData<Boolean> get() = _isChannelNumberKeyboardVisible

    init {
        _isChannelListVisible.value = false
    }

    fun updateMediaInfo(newMediaInfo: MediaInfo) {
        _mediaInfo.value = newMediaInfo
    }

    fun updateChannelNumber(newChannelNumber: Int) {
        _channelNumber.value = newChannelNumber
    }

    fun updateChannelName(newChannelName: String) {
        _channelName.value = newChannelName
    }

    fun updateTimeDate() {
        _timeDate.value = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
    }

    fun updateErrorMessage(newErrorMessage: String) {
        _errorMessage.value = newErrorMessage
    }



    fun showChannelList() {
        _isChannelListVisible.value = true
    }

    fun hideChannelList() {
        _isChannelListVisible.value = false
    }

    fun showSettingsMenu() {
        _isSettingsMenuVisible.value = true
    }

    fun hideSettingsMenu() {
        _isSettingsMenuVisible.value = false
    }

    fun showTrackMenu() {
        _isTrackMenuVisible.value = true
    }

    fun hideTrackMenu() {
        _isTrackMenuVisible.value = false
    }

    fun hideMediaInfo() {
        _isMediaInfoVisible.value = false
    }

    fun showMediaInfo() {
        _isMediaInfoVisible.value = true
    }

    fun showChannelNumber() {
        _isChannelNumberVisible.value = true
    }

    fun hideChannelNumber() {
        _isChannelNumberVisible.value = false
    }

    fun showChannelName() {
        _isChannelNameVisible.value = true
    }

    fun hideChannelName() {
        _isChannelNameVisible.value = false
    }

    fun showTimeDate() {
        _isTimeDateVisible.value = true
    }

    fun hideTimeDate() {
        _isTimeDateVisible.value = false
    }

    fun showErrorMessage() {
        _isErrorMessageVisible.value = true
    }

    fun hideErrorMessage() {
        _isErrorMessageVisible.value = false
    }

    fun showPlayer() {
        _isPlayerVisible.value = true
    }

    fun hidePlayer() {
        _isPlayerVisible.value = false
    }

    fun showButtonUp() {
        _isButtonUpVisible.value = true
    }

    fun hideButtonUp() {
        _isButtonUpVisible.value = false
    }

    fun showButtonDown() {
        _isButtonDownVisible.value = true
    }

    fun hideButtonDown() {
        _isButtonDownVisible.value = false
    }

    fun showButtonSettings() {
        _isButtonSettingsVisible.value = true
    }

    fun hideButtonSettings() {
        _isButtonSettingsVisible.value = false
    }

    fun showButtonChannelList() {
        _isButtonChannelListVisible.value = true
    }

    fun hideButtonChannelList() {
        _isButtonChannelListVisible.value = false
    }

    fun showButtonPiP() {
        _isButtonPiPVisible.value = true
    }

    fun hideButtonPiP() {
        _isButtonPiPVisible.value = false
    }

    fun showChannelNumberKeyboard() {
        _isChannelNumberKeyboardVisible.value = true
    }

    fun hideChannelNumberKeyboard() {
        _isChannelNumberKeyboardVisible.value = false
    }

    fun showBottomInfo() {
        _isBottomInfoVisible.value = true
    }

    fun hideBottomInfo() {
        _isBottomInfoVisible.value = false
    }

    fun updateSourcesTriedCount(i: Int) {
        _sourcesTriedCount.value = i
    }

    fun updateTriesCountForEachSource(i: Int) {
        _triesCountForEachSource.value = i
    }

    fun updateCurrentItemSelectedFromChannelList(i: Int) {
        _currentItemSelectedFromChannelList.value = i
    }

    fun updateCurrentItemSelectedFromChannelSettingsMenu(i: Int) {
        _currentItemSelectedFromChannelSettingsMenu.value = i
    }

    fun updateCurrentItemSelectedFromAudioTracksMenu(i: Int) {
        _currentItemSelectedFromAudioTracksMenu.value = i
    }

    fun updateCurrentItemSelectedFromVideoTracksMenu(i: Int) {
        _currentItemSelectedFromVideoTracksMenu.value = i
    }

    fun updateCurrentItemSelectedFromSubtitlesTracksMenu(i: Int) {
        _currentItemSelectedFromSubtitlesTracksMenu.value = i
    }

    fun updateCurrentItemSelectedFromChannelSourcesMenu(i: Int) {
        _currentItemSelectedFromChannelSourcesMenu.value = i
    }

    fun updateCurrentLoadedMenuSetting(i: Int){
        _currentLoadedMenuSetting.value = i
    }

    fun updateIsSourceForced(isSourceForced: Boolean) {
        _isSourceForced.value = isSourceForced
    }

    fun setIsBuffering(isBuffering: Boolean) {
        _isBuffering.value = isBuffering
    }

    fun setIsLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }


    fun updateCurrentStreamSource(newStreamSource: StreamSourceItem) {
        _currentStreamSource.value = newStreamSource
    }
}