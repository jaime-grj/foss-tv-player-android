package com.gaarx.iptvplayer.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gaarx.iptvplayer.domain.model.MediaInfo
import com.gaarx.iptvplayer.domain.model.StreamSourceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(): ViewModel() {

    private val _currentStreamSource = MutableLiveData<StreamSourceItem>()
    val currentStreamSource: LiveData<StreamSourceItem> get() = _currentStreamSource

    private val _isSourceForced = MutableLiveData<Boolean>()
    val isSourceForced: LiveData<Boolean> get() = _isSourceForced

    private val _isQualityForced = MutableLiveData<Boolean>()
    val isQualityForced: LiveData<Boolean> get() = _isQualityForced

    private val _sourcesTriedCount = MutableLiveData<Int>()
    val sourcesTriedCount: LiveData<Int> get() = _sourcesTriedCount

    private val _triesCountForEachSource = MutableLiveData<Int>()
    val triesCountForEachSource: LiveData<Int> get() = _triesCountForEachSource

    private val _isSourceLoading = MutableLiveData<Boolean>()
    val isSourceLoading: LiveData<Boolean> get() = _isSourceLoading

    private val _isChannelLoading = MutableLiveData<Boolean>()
    val isChannelLoading: LiveData<Boolean> get() = _isChannelLoading

    private val _isBuffering = MutableLiveData<Boolean>()
    val isBuffering: LiveData<Boolean> get() = _isBuffering

    private val _currentItemSelectedFromChannelList: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromChannelList: LiveData<Int> get() = _currentItemSelectedFromChannelList

    private val _currentItemSelectedFromChannelSettingsMenu: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromChannelSettingsMenu: LiveData<Int> get() = _currentItemSelectedFromChannelSettingsMenu

    private val _currentItemSelectedFromCategoryList: MutableLiveData<Int> = MutableLiveData()
    val currentItemSelectedFromCategoryList: LiveData<Int> get() = _currentItemSelectedFromCategoryList

    private val _currentLoadedMenuSetting: MutableLiveData<Int> = MutableLiveData()
    val currentLoadedMenuSetting: LiveData<Int> get() = _currentLoadedMenuSetting

    private val _isNumberListMenuVisible = MutableLiveData<Boolean>()
    val isNumberListMenuVisible: LiveData<Boolean> get() = _isNumberListMenuVisible

    fun onCreate() {
        _isSourceForced.value = false
        _isQualityForced.value = false
        _sourcesTriedCount.value = 0
        _triesCountForEachSource.value = 0
        _isSourceLoading.value = false
        _isBuffering.value = false
        _currentItemSelectedFromChannelList.value = -1
        _currentItemSelectedFromChannelSettingsMenu.value = 0
        _currentItemSelectedFromCategoryList.value = 0
        _currentLoadedMenuSetting.value = -1
    }

    private val _mediaInfo = MutableLiveData<MediaInfo>()
    val mediaInfo: LiveData<MediaInfo> get() = _mediaInfo

    private val _channelNumber = MutableLiveData<Int>()
    val channelNumber: LiveData<Int> get() = _channelNumber

    private val _channelName = MutableLiveData<String>()
    val channelName: LiveData<String> get() = _channelName

    private val _categoryName = MutableLiveData<String>()
    val categoryName: LiveData<String> get() = _categoryName

    private val _timeDate = MutableLiveData<String>()
    val timeDate: LiveData<String> get() = _timeDate

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _bottomErrorMessage = MutableLiveData<String>()
    val bottomErrorMessage: LiveData<String> get() = _bottomErrorMessage

    private val _isAnimatedLoadingIconVisible = MutableLiveData<Boolean>()
    val isAnimatedLoadingIconVisible: LiveData<Boolean> get() = _isAnimatedLoadingIconVisible

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

    private val _isBottomErrorMessageVisible = MutableLiveData<Boolean>()
    val isBottomErrorMessageVisible: LiveData<Boolean> get() = _isBottomErrorMessageVisible

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

    private val _isButtonCategoryListVisible = MutableLiveData<Boolean>()
    val isButtonCategoryListVisible: LiveData<Boolean> get() = _isButtonCategoryListVisible

    private val _isChannelNumberKeyboardVisible = MutableLiveData<Boolean>()
    val isChannelNumberKeyboardVisible: LiveData<Boolean> get() = _isChannelNumberKeyboardVisible

    private val _isCategoryListVisible = MutableLiveData<Boolean>()
    val isCategoryListVisible: LiveData<Boolean> get() = _isCategoryListVisible

    private val _isCategoryNameVisible = MutableLiveData<Boolean>()
    val isCategoryNameVisible: LiveData<Boolean> get() = _isCategoryNameVisible

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

    fun updateBottomErrorMessage(newErrorMessage: String) {
        _bottomErrorMessage.value = newErrorMessage
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

    fun showBottomErrorMessage() {
        _isBottomErrorMessageVisible.value = true
    }

    fun hideBottomErrorMessage() {
        _isBottomErrorMessageVisible.value = false
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

    fun showButtonCategoryList() {
        _isButtonCategoryListVisible.value = true
    }

    fun hideButtonCategoryList() {
        _isButtonCategoryListVisible.value = false
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

    fun updateCurrentItemSelectedFromCategoryList(i: Int) {
        _currentItemSelectedFromCategoryList.value = i
    }

    fun updateCurrentLoadedMenuSetting(i: Int){
        _currentLoadedMenuSetting.value = i
    }

    fun updateIsSourceForced(isSourceForced: Boolean) {
        _isSourceForced.value = isSourceForced
    }

    fun updateIsQualityForced(isQualityForced: Boolean) {
        _isQualityForced.value = isQualityForced
    }

    fun setIsBuffering(isBuffering: Boolean) {
        _isBuffering.value = isBuffering
    }

    fun setIsSourceLoading(isLoading: Boolean) {
        _isSourceLoading.value = isLoading
    }

    fun setIsChannelLoading(isLoading: Boolean) {
        _isChannelLoading.value = isLoading
    }

    fun updateCurrentStreamSource(newStreamSource: StreamSourceItem) {
        _currentStreamSource.value = newStreamSource
    }

    fun hideAnimatedLoadingIcon() {
        _isAnimatedLoadingIconVisible.value = false
    }

    fun showAnimatedLoadingIcon() {
        _isAnimatedLoadingIconVisible.value = true
    }

    fun showCategoryList() {
        _isCategoryListVisible.value = true
    }

    fun hideCategoryList() {
        _isCategoryListVisible.value = false
    }

    fun showCategoryName() {
        _isCategoryNameVisible.value = true
    }

    fun hideCategoryName() {
        _isCategoryNameVisible.value = false
    }

    fun updateCategoryName(name: String) {
        _categoryName.value = name
    }

    fun showNumberListMenu(){
        _isNumberListMenuVisible.value = true
    }

    fun hideNumberListMenu(){
        _isNumberListMenuVisible.value = false
    }

    private val _incomingChannelId = MutableStateFlow<Long?>(null)
    val incomingChannelId: StateFlow<Long?> = _incomingChannelId.asStateFlow()

    fun setIncomingChannelId(id: Long) {
        _incomingChannelId.value = id
    }
}