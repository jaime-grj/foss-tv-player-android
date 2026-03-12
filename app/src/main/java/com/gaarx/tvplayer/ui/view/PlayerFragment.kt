package com.gaarx.tvplayer.ui.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Rational
import android.view.Display
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gaarx.tvplayer.R
import com.gaarx.tvplayer.core.MediaUtils
import com.gaarx.tvplayer.databinding.FragmentPlayerBinding
import com.gaarx.tvplayer.domain.model.CategoryItem
import com.gaarx.tvplayer.domain.model.ChannelItem
import com.gaarx.tvplayer.domain.model.ChannelSettings
import com.gaarx.tvplayer.domain.model.DrmTypeItem
import com.gaarx.tvplayer.domain.model.MediaInfo
import com.gaarx.tvplayer.domain.model.StreamSourceItem
import com.gaarx.tvplayer.domain.model.StreamSourceTypeItem
import com.gaarx.tvplayer.ui.adapters.AudioTracksAdapter
import com.gaarx.tvplayer.ui.adapters.CategoryListAdapter
import com.gaarx.tvplayer.ui.adapters.ChannelListAdapter
import com.gaarx.tvplayer.ui.adapters.ChannelSettingsAdapter
import com.gaarx.tvplayer.ui.adapters.ChannelSourcesAdapter
import com.gaarx.tvplayer.ui.adapters.NumberListAdapter
import com.gaarx.tvplayer.ui.adapters.SubtitlesTracksAdapter
import com.gaarx.tvplayer.ui.adapters.VideoTracksAdapter
import com.gaarx.tvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.tvplayer.ui.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.flow.collectLatest
import com.gaarx.tvplayer.core.Constants.DEFAULT_REFRESH_RATE
import com.gaarx.tvplayer.core.Constants.MAX_DIGITS
import kotlinx.coroutines.suspendCancellableCoroutine

@UnstableApi
@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector

    private var mediaInfo: MediaInfo = MediaInfo()

    private lateinit var jobLoadStreamSource: Job
    private lateinit var jobLoadChannel: Job
    private var jobSwitchChannel : Job? = null

    private lateinit var rvChannelList: VerticalGridView
    private lateinit var rvChannelSettings: RecyclerView
    private lateinit var rvAudioTracks: VerticalGridView
    private lateinit var rvSubtitlesTracks: VerticalGridView
    private lateinit var rvChannelSources: VerticalGridView
    private lateinit var rvVideoTracks: VerticalGridView
    private lateinit var rvCategoryList: VerticalGridView
    private lateinit var rvNumberList: RecyclerView

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val channelViewModel: ChannelViewModel by activityViewModels()

    private var isInEPGPictureInPictureMode: Boolean = false

    private var timerManager = PlayerTimerManager()
    private lateinit var lifecycleManager: PlayerLifecycleManager
    private lateinit var playerManager: PlayerManager
    private lateinit var streamSourceManager: StreamSourceManager
    private lateinit var keyHandler: KeyEventHandler
    private lateinit var animationHelper: LoadingAnimationHelper
    private lateinit var uiController: PlayerUIController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragmentManager.setFragmentResultListener("channelRequestKey", this) { _, bundle ->
            val channelId = bundle.getLong("channelId", 0L)
            Log.d("PlayerFragment", "Received channelId: $channelId")
            playerViewModel.setIncomingChannelId(channelId)
            isInEPGPictureInPictureMode = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        channelViewModel.updateIsLoadingChannel(true)
        switchRefreshRate(DEFAULT_REFRESH_RATE)
        initUI()
        playerViewModel.showAnimatedLoadingIcon()

        lifecycleManager = PlayerLifecycleManager(timerManager)
        lifecycle.addObserver(lifecycleManager)

        initPlayer()

        streamSourceManager = StreamSourceManager(
            playerManager,
            playerViewModel,
            timerManager,
            lifecycleScope
        )

        channelViewModel.isImportingData.observe(viewLifecycleOwner) { isImportingData ->
            if (isImportingData == true) {
                playerViewModel.showAnimatedLoadingIcon()
            } else {
                playerViewModel.hideAnimatedLoadingIcon()
            }
        }

        initAudioTracksMenu()
        initSubtitlesTracksMenu()
        initNumberList()

        binding.buttonPiP.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPiPModeIfSupported()
            } else {
                Toast.makeText(activity, "Picture-in-Picture mode is not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            playerViewModel.incomingChannelId.collectLatest { channelId ->
                if (channelId != null) {
                    channelViewModel.updateIsLoadingChannel(true)
                    val newChannel = channelViewModel.getChannelById(channelId)
                    if (newChannel != null) {
                        playerViewModel.updateCurrentCategoryId(-1L)
                        channelViewModel.updateLastCategoryLoaded(-1L)
                        playerViewModel.updateCategoryName("Favoritos")
                        channelViewModel.updateIsLoadingChannel(false)
                        loadChannel(newChannel)
                    }
                } else {
                    val lastChannelId = channelViewModel.getLastChannelLoaded()
                    val lastCategoryId = channelViewModel.getLastCategoryLoaded()
                    Log.i("PlayerActivity", "lastChannelId: $lastChannelId. lastCategoryId: $lastCategoryId")

                    val channelCount = channelViewModel.getChannelCount()
                    if (channelCount == 0) {
                        val url = loadConfigURLDialogSuspend()
                        if (!url.isNullOrEmpty()) {
                            channelViewModel.updateIsImportingData(true)
                            val updated = channelViewModel.importJSONData()
                            channelViewModel.updateIsImportingData(false)
                            if (!updated) {
                                Toast.makeText(requireContext(), "Error al cargar la lista de canales", Toast.LENGTH_SHORT).show()
                            } else {
                                channelViewModel.updateEPG()
                                playerViewModel.updateCurrentCategoryId(-1L)
                                playerViewModel.updateCategoryName("")
                                try {
                                    loadChannel(channelViewModel.getChannel(-1L, 1))
                                } catch (e: Exception) {
                                    Log.e("PlayerActivity", "Error: ${e.message}")
                                }
                            }
                        }
                    } else if (lastChannelId != 0L && lastCategoryId != 0L) {
                        val channel = channelViewModel.getChannelById(lastChannelId)
                        if (channel != null) {
                            playerViewModel.updateCurrentCategoryId(lastCategoryId)
                            val category = channelViewModel.getCategoryById(lastCategoryId)
                            if (category != null) {
                                playerViewModel.updateCategoryName(category.name)
                            }
                            loadChannel(channel)
                        } else {
                            playerViewModel.updateCurrentCategoryId(-1L)
                            playerViewModel.updateCategoryName("")
                            loadChannel(channelViewModel.getPreviousChannel(-1L, 1))
                        }
                    } else {
                        playerViewModel.updateCurrentCategoryId(-1L)
                        playerViewModel.updateCategoryName("")
                        loadChannel(channelViewModel.getPreviousChannel(-1L, 1))
                    }
                    channelViewModel.updateIsLoadingChannel(false)
                    initCategoryList()
                }
            }
        }

        keyHandler = KeyEventHandler(
            binding,
            requireContext(),
            playerViewModel,
            channelViewModel,
            rvChannelList,
            rvChannelSettings,
            rvCategoryList,
            rvAudioTracks,
            rvSubtitlesTracks,
            rvChannelSources,
            rvVideoTracks,
            rvNumberList,
            playerManager,
            lifecycleScope,
            onLoadChannel = { loadChannel(it) },
            onLoadNextChannel = { loadNextChannel() },
            onLoadPreviousChannel = { loadPreviousChannel() },
            onLoadStreamSource = { loadStreamSource(it) },
            onShowChannelInfoWithTimeout = { uiController.showChannelInfoWithTimeout() },
            onShowFullChannelUIWithTimeout = { timeout -> uiController.showFullChannelUIWithTimeout(timeout) },
            onShowChannelNumberWithTimeoutAndChangeChannel = { uiController.showChannelNumberWithTimeoutAndChangeChannel { loadChannel(it) } },
            onLoadSetting = { loadSetting(it) },
            onInitSettingsMenu = { initSettingsMenu() },
            onInitChannelList = { initChannelList() },
            requireActivity()
        )
    }

    private fun initPlayer() {
        playerManager = PlayerManager(
            context = requireContext(),
            binding,
            playerViewModel,
            channelViewModel,
            mediaInfo,
            timerManager,
            onIsPlayingChanged = {
                when (playerViewModel.currentLoadedMenuSetting.value) {
                    ChannelSettings.AUDIO_TRACKS -> {
                        val audioTrackList = playerManager.getAudioTracks()
                        val audioTrackLoader = playerManager.getAudioTrackLoader()
                        rvAudioTracks.adapter = AudioTracksAdapter(audioTrackList, audioTrackLoader)
                        rvAudioTracks.requestFocus()
                    }
                    ChannelSettings.SUBTITLES_TRACKS -> {
                        val subtitlesTrackList = playerManager.getSubtitlesTracks()
                        val subtitlesTrackLoader = playerManager.getSubtitlesTrackLoader()
                        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(subtitlesTrackList, subtitlesTrackLoader)
                        rvSubtitlesTracks.requestFocus()
                    }
                    ChannelSettings.VIDEO_TRACKS -> {
                        val isQualityForced = playerViewModel.isQualityForced.value == true
                        val videoTrackList = playerManager.getVideoTracks(isQualityForced)
                        val videoTrackLoader = playerManager.getVideoTrackLoader()
                        rvVideoTracks.adapter = VideoTracksAdapter(isQualityForced, videoTrackList, videoTrackLoader)
                        rvVideoTracks.requestFocus()
                    }
                }
            },
            onVideoFormatChanged = { refreshRate ->
                switchRefreshRate(refreshRate)
            },
            onTryNextStreamSource = {
                val channel = playerViewModel.currentChannel.value
                val source = playerViewModel.currentStreamSource.value
                if (channel != null && source != null) {
                    lifecycleScope.launch {
                        streamSourceManager.tryNextStreamSource(channel, source)
                    }
                }
            }
        )

        player = playerManager.init()
        trackSelector = playerManager.trackSelector
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        playerViewModel.onCreate()
        animationHelper = LoadingAnimationHelper(binding)
        uiController = PlayerUIController(requireContext(), playerViewModel, channelViewModel, lifecycleScope)
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
        playerViewModel.updateCurrentItemSelectedFromChannelList(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(activity?.window!!, false)
            activity?.window?.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars() or WindowInsets.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity?.window?.decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
        lifecycleScope.launch {
            val lastDownloadedTime = channelViewModel.getEPGLastDownloadedTime()
            if (lastDownloadedTime <= 0L || System.currentTimeMillis() - lastDownloadedTime > 2 * 60 * 60 * 1000) {
                channelViewModel.downloadEPG()
            }
        }
    }

    private fun setupRecyclerViews(){
        rvChannelList = binding.channelList
        rvChannelSettings = binding.rvChannelSettings
        rvAudioTracks = binding.rvChannelTrackSettings
        rvSubtitlesTracks = binding.rvChannelTrackSettings
        rvChannelSources = binding.rvChannelTrackSettings
        rvVideoTracks = binding.rvChannelTrackSettings
        rvCategoryList = binding.rvCategoryList
        rvNumberList = binding.rvNumberList
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers(){
        playerViewModel.channelName.observe(viewLifecycleOwner) { channelName ->
            binding.channelName.text = channelName
        }

        playerViewModel.channelNumber.observe(viewLifecycleOwner) { channelNumber ->
            binding.channelNumber.text = channelNumber.toString()
        }

        playerViewModel.categoryName.observe(viewLifecycleOwner) { categoryName ->
            binding.categoryName.text = categoryName
        }

        playerViewModel.timeDate.observe(viewLifecycleOwner) { timeDate ->
            binding.timeDate.text = timeDate
        }

        playerViewModel.mediaInfo.observe(viewLifecycleOwner) { mediaInfo ->
            binding.channelVideoResolution.apply {
                isVisible = mediaInfo.videoResolution != null
                text = mediaInfo.videoResolution
            }
            binding.channelVideoAspectRatio.apply {
                isVisible = mediaInfo.videoAspectRatio != null
                text = mediaInfo.videoAspectRatio
            }
            binding.channelVideoQuality.apply {
                isVisible = mediaInfo.videoQuality != null
                text = mediaInfo.videoQuality
            }
            binding.channelVideoCodec.apply {
                isVisible = mediaInfo.videoCodec != null
                text = mediaInfo.videoCodec
            }
            binding.channelVideoBitrate.apply {
                isVisible = mediaInfo.videoBitrate != null
                text = mediaInfo.videoBitrate
            }
            binding.channelVideoFrameRate.apply {
                isVisible = mediaInfo.videoFrameRate != null
                text = mediaInfo.videoFrameRate
            }
            binding.channelAudioCodec.apply {
                isVisible = mediaInfo.audioCodec != null
                text = mediaInfo.audioCodec
            }
            binding.channelAudioBitrate.apply {
                isVisible = mediaInfo.audioBitrate != null
                text = mediaInfo.audioBitrate
            }
            binding.channelAudioSamplingRate.apply {
                isVisible = mediaInfo.audioSamplingRate != null
                text = mediaInfo.audioSamplingRate
            }
            binding.channelAudioChannels.apply {
                isVisible = mediaInfo.audioChannels != null
                text = mediaInfo.audioChannels
            }
            binding.channelHasSubtitles.isVisible = mediaInfo.hasSubtitles
            binding.channelHasEPG.isVisible = mediaInfo.hasEPG
            binding.channelHasMultiLanguageAudio.isVisible = mediaInfo.hasMultipleAudios
            binding.channelHasAudioDescription.isVisible = mediaInfo.hasAudioDescription
            binding.channelHasTeletext.isVisible = mediaInfo.hasTeletext
            binding.channelHasDRM.isVisible = mediaInfo.hasDRM
        }

        playerViewModel.bottomErrorMessage.observe(viewLifecycleOwner) { bottomErrorMessage ->
            binding.bottomErrorMessage.text = bottomErrorMessage
        }

        playerViewModel.isAnimatedLoadingIconVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) animationHelper.showLoadingDots() else animationHelper.hideLoadingDots()
        }

        binding.playerView.bindVisibility(viewLifecycleOwner, playerViewModel.isPlayerVisible)
        binding.buttonUp.bindVisibility(viewLifecycleOwner, playerViewModel.isButtonUpVisible)
        binding.buttonDown.bindVisibility(viewLifecycleOwner, playerViewModel.isButtonDownVisible)
        binding.buttonSettings.bindVisibility(viewLifecycleOwner, playerViewModel.isButtonSettingsVisible)
        binding.buttonChannelList.bindVisibility(viewLifecycleOwner, playerViewModel.isButtonChannelListVisible)
        binding.buttonPiP.bindVisibility(viewLifecycleOwner, playerViewModel.isButtonPiPVisible)
        binding.buttonCategory.bindVisibility(viewLifecycleOwner, playerViewModel.isButtonCategoryListVisible)
        binding.channelName.bindVisibility(viewLifecycleOwner, playerViewModel.isChannelNameVisible)
        binding.channelNumber.bindVisibility(viewLifecycleOwner, playerViewModel.isChannelNumberVisible)
        binding.channelNumberKeyboard.bindVisibility(viewLifecycleOwner, playerViewModel.isChannelNumberKeyboardVisible)
        binding.channelBottomInfo.bindVisibility(viewLifecycleOwner, playerViewModel.isBottomInfoVisible)
        binding.channelMediaInfo.bindVisibility(viewLifecycleOwner, playerViewModel.isMediaInfoVisible)
        binding.channelInfoBackground.bindVisibility(viewLifecycleOwner, orLiveData(playerViewModel.isChannelNumberVisible, playerViewModel.isChannelNumberKeyboardVisible))
        binding.categoryName.bindVisibility(viewLifecycleOwner, playerViewModel.isCategoryNameVisible)
        binding.timeDate.bindVisibility(viewLifecycleOwner, playerViewModel.isTimeDateVisible)
        binding.message.bindVisibility(viewLifecycleOwner, playerViewModel.isErrorMessageVisible)
        binding.bottomErrorMessage.bindVisibility(viewLifecycleOwner, playerViewModel.isBottomErrorMessageVisible)
        binding.rvNumberList.bindVisibility(viewLifecycleOwner, playerViewModel.isNumberListMenuVisible)
        binding.channelList.bindVisibility(viewLifecycleOwner, playerViewModel.isChannelListVisible)
        binding.rvChannelSettings.bindVisibility(viewLifecycleOwner, playerViewModel.isSettingsMenuVisible)
        binding.rvChannelTrackSettings.bindVisibility(viewLifecycleOwner, playerViewModel.isTrackMenuVisible)

        channelViewModel.currentProgram.observe(viewLifecycleOwner) { currentProgram ->
            if (currentProgram != null){
                val currentProgramStartTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentProgram.startTime)
                val currentProgramStopTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentProgram.stopTime)
                val currentTime = System.currentTimeMillis()
                val currentProgramDuration = currentProgram.stopTime.time.minus(currentProgram.startTime.time)
                val progress = (currentTime - currentProgram.startTime.time) * 100 / currentProgramDuration
                val ageRatingIcon = currentProgram.ageRatingIcon
                val ageRatingText = currentProgram.ageRating

                binding.ivAgeRating.isVisible = !ageRatingIcon.isNullOrBlank()
                binding.tvAgeRating.apply {
                    isVisible = ageRatingIcon.isNullOrBlank() && ageRatingText != null
                    text = ageRatingText
                }
                
                binding.progressBar.progress = progress.toInt()
                binding.tvChannelCurrentProgram.text = currentProgram.title
                binding.tvChannelCurrentProgram.isVisible = true
                binding.progressBar.isVisible = true
                binding.tvStartTime.isVisible = true
                binding.tvEndTime.isVisible = true
                binding.tvStartTime.text = currentProgramStartTimeFormatted
                binding.tvEndTime.text = currentProgramStopTimeFormatted
                
                Glide.with(this)
                    .load(ageRatingIcon)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.ivAgeRating)

                if (playerViewModel.isChannelNameVisible.value == true){
                    binding.channelBottomInfo.isVisible = true
                }
            } else{
                if (channelViewModel.nextProgram.value == null){
                    binding.channelBottomInfo.isVisible = false
                    mediaInfo.hasEPG = false
                    playerViewModel.updateMediaInfo(mediaInfo)
                }
                binding.progressBar.isVisible = false
                binding.tvStartTime.isVisible = false
                binding.tvEndTime.isVisible = false
                binding.tvChannelCurrentProgram.isVisible = false
                binding.ivAgeRating.isVisible = false
                binding.tvAgeRating.isVisible = false
            }
        }

        channelViewModel.nextProgram.observe(viewLifecycleOwner) { nextProgram ->
            if (nextProgram != null){
                val nextProgramStartTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextProgram.startTime)
                val nextProgramStopTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextProgram.stopTime)
                binding.tvChannelNextProgram.text = getString(R.string.nextProgram) + nextProgram.title + " (" +  nextProgramStartTimeFormatted + " - " + nextProgramStopTimeFormatted + ")"
                binding.tvChannelNextProgram.isVisible = true
                if (channelViewModel.currentProgram.value == null && playerViewModel.isChannelNameVisible.value == true){
                    binding.channelBottomInfo.isVisible = true
                }
            } else {
                if (channelViewModel.currentProgram.value == null){
                    binding.channelBottomInfo.isVisible = false
                    mediaInfo.hasEPG = false
                    playerViewModel.updateMediaInfo(mediaInfo)
                }
                binding.tvChannelNextProgram.isVisible = false
            }
        }

        channelViewModel.currentProgram.observe(viewLifecycleOwner) { currentProgram ->
            mediaInfo.hasEPG = currentProgram != null || channelViewModel.nextProgram.value != null
            playerViewModel.updateMediaInfo(mediaInfo)
        }

        playerViewModel.isCategoryListVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.rvCategoryList.isVisible = isVisible
        }
    }

    private fun setupClickListeners(){
        binding.playerView.setOnClickListener {
            when {
                playerViewModel.isSettingsMenuVisible.value == true -> playerViewModel.hideSettingsMenu()
                playerViewModel.isChannelListVisible.value == true -> playerViewModel.hideChannelList()
                playerViewModel.isTrackMenuVisible.value == true -> playerViewModel.hideTrackMenu()
                playerViewModel.isChannelNumberVisible.value == true -> {
                    uiController.hideStandardButtons()
                    uiController.hideChannelInfo()
                    uiController.hideMediaAndBottomInfo()
                }
                playerViewModel.isCategoryListVisible.value == true -> playerViewModel.hideCategoryList()
                else -> uiController.showFullChannelUIWithTimeout()
            }
        }

        binding.buttonUp.setOnClickListener { loadNextChannel() }
        binding.buttonDown.setOnClickListener { loadPreviousChannel() }
        binding.buttonSettings.setOnClickListener {
            if (playerViewModel.isSettingsMenuVisible.value == true) {
                playerViewModel.hideSettingsMenu()
            } else {
                initSettingsMenu()
                playerViewModel.showSettingsMenu()
            }
        }

        binding.buttonChannelList.setOnClickListener {
            if (playerViewModel.isChannelListVisible.value == true) {
                playerViewModel.hideChannelList()
            } else {
                lifecycleScope.launch {
                    initChannelList()
                    playerViewModel.showChannelList()
                }
            }
        }

        binding.buttonCategory.setOnClickListener {
            if (playerViewModel.isCategoryListVisible.value == true) {
                playerViewModel.hideCategoryList()
            } else {
                playerViewModel.showCategoryList()
                rvCategoryList.requestFocus()
            }
        }
    }

    private fun switchRefreshRate(frameRate: Float) {
        val displayManager = activity?.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val supportedModes = display.supportedModes
        val mode = supportedModes.find { MediaUtils.areRatesEqual(frameRate, it.refreshRate) }

        if (mode != null) {
            val attributes = activity?.window?.attributes
            attributes?.preferredDisplayModeId = mode.modeId
            activity?.window?.attributes = attributes
            Log.i("DisplayMode", "Switched to refresh rate: ${mode.refreshRate}")
        } else {
            Log.i("DisplayMode", "No matching refresh rate found for $frameRate fps")
        }
    }

    private fun initSettingsMenu() {
        val settingsList = listOf(
            ChannelSettings(getString(R.string.change_source)),
            ChannelSettings(getString(R.string.audio_track)),
            ChannelSettings(getString(R.string.subtitle_track)),
            ChannelSettings(getString(R.string.video_track)),
            ChannelSettings(getString(R.string.settings_aspect_ratio)),
            ChannelSettings(getString(R.string.settings_update_epg)),
            ChannelSettings(getString(R.string.settings_epg)),
            ChannelSettings(getString(R.string.settings_update_channel_list)),
            ChannelSettings(getString(R.string.settings_config_url))
        )
        rvChannelSettings.layoutManager = LinearLayoutManager(requireContext())
        rvChannelSettings.adapter = ChannelSettingsAdapter(settingsList) { selectedSetting ->
            if (playerViewModel.isSettingsMenuVisible.value == true) {
                playerViewModel.hideSettingsMenu()
            }
            loadSetting(selectedSetting)
        }
    }

    private fun initAudioTracksMenu() {
        rvAudioTracks.adapter = AudioTracksAdapter(listOf()) { selectedAudioTrack ->
            if (playerViewModel.isSettingsMenuVisible.value == true) {
                playerViewModel.hideSettingsMenu()
            }
            playerManager.loadAudioTrack(selectedAudioTrack)
        }
    }

    private fun initSubtitlesTracksMenu() {
        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(listOf()) { selectedSubtitlesTrack ->
            playerManager.loadSubtitlesTrack(selectedSubtitlesTrack)
        }
    }

    private fun loadSetting(settingIndex: Int) {
        when (settingIndex) {
            ChannelSettings.AUDIO_TRACKS -> loadAudioTracksMenu()
            ChannelSettings.SUBTITLES_TRACKS -> loadSubtitlesTracksMenu()
            ChannelSettings.VIDEO_TRACKS -> loadVideoTracksMenu()
            ChannelSettings.SOURCES -> loadSourcesMenu()
            ChannelSettings.UPDATE_EPG -> channelViewModel.updateEPG()
            ChannelSettings.SHOW_EPG -> {
                val mainActivity = requireActivity() as MainActivity
                mainActivity.showPipOverlay(player)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, EpgFragment())
                    .addToBackStack(null)
                    .commit()
                isInEPGPictureInPictureMode = true
            }
            ChannelSettings.UPDATE_CHANNEL_LIST -> {
                lifecycleScope.launch {
                    val updated = channelViewModel.importJSONData()
                    if (!updated) {
                        Toast.makeText(requireContext(), "Error al cargar la lista de canales", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    initCategoryList()
                    playerViewModel.updateCurrentCategoryId(-1L)
                    playerViewModel.updateCategoryName("Favoritos")
                    try {
                        val firstChannel = channelViewModel.getChannel(-1L, 1)
                        loadChannel(firstChannel)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al cargar la lista de canales: $e", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ChannelSettings.ASPECT_RATIO -> loadAspectRatioMenu()
            ChannelSettings.CONFIG_URL -> {
                lifecycleScope.launch {
                    val url = loadConfigURLDialogSuspend()
                    if (!url.isNullOrEmpty()) {
                        channelViewModel.updateConfigURL(url)
                    }
                }
            }
        }
    }

    private fun loadAspectRatioMenu() {
        val aspectRatios = listOf("Auto", "Fill", "Zoom")
        val aspectRatioValues = listOf(AspectRatio.AUTO, AspectRatio.FILL, AspectRatio.ZOOM)
        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona relación de aspecto")
            .setItems(aspectRatios.toTypedArray()) { _, which ->
                applyAspectRatio(aspectRatioValues[which])
            }.show()
    }

    private fun applyAspectRatio(aspectRatio: AspectRatio) {
        binding.playerView.resizeMode = when (aspectRatio) {
            AspectRatio.AUTO -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            AspectRatio.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatio.ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    enum class AspectRatio { AUTO, FILL, ZOOM }

    private suspend fun loadConfigURLDialogSuspend(): String? = suspendCancellableCoroutine { cont ->
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.dialog_config_url_hint)
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }

        lifecycleScope.launch {
            val url = channelViewModel.getConfigURL()
            editText.setText(url)
            editText.setSelection(editText.text.length)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_config_url))
            .setView(editText)
            .setPositiveButton(getString(R.string.dialog_config_url_accept)) { dialogInterface, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    cont.resume(url) { _, _, _ -> dialogInterface.dismiss() }
                } else {
                    cont.resume(null) { _, _, _ -> dialogInterface.dismiss() }
                }
            }
            .setNegativeButton(getString(R.string.dialog_config_url_cancel)) { dialogInterface, _ ->
                cont.resume(null) { _, _, _ -> dialogInterface.dismiss() }
            }
            .create()

        dialog.setOnCancelListener { cont.resume(null) { _, _, _ -> dialog.dismiss() } }
        cont.invokeOnCancellation { if (dialog.isShowing) dialog.dismiss() }
        dialog.show()
    }

    private fun loadAudioTracksMenu(){
        val audioTrackList = playerManager.getAudioTracks()
        rvAudioTracks.adapter = AudioTracksAdapter(audioTrackList) { selectedAudioTrack ->
            playerManager.loadAudioTrack(selectedAudioTrack)
        }
        playerViewModel.showTrackMenu()
        rvAudioTracks.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.AUDIO_TRACKS)
    }

    private fun loadSubtitlesTracksMenu() {
        val subtitlesTrackList = playerManager.getSubtitlesTracks()
        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(subtitlesTrackList) { selectedSubtitlesTrack ->
            playerManager.loadSubtitlesTrack(selectedSubtitlesTrack)
        }
        playerViewModel.showTrackMenu()
        rvSubtitlesTracks.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.SUBTITLES_TRACKS)
    }

    private fun loadVideoTracksMenu() {
        val isQualityForced = playerViewModel.isQualityForced.value == true
        val videoTracksList = playerManager.getVideoTracks(isQualityForced)
        rvVideoTracks.adapter = VideoTracksAdapter(isQualityForced, videoTracksList) { selectedVideoTrack ->
            playerViewModel.updateIsQualityForced(selectedVideoTrack.id.toInt() != -1)
            playerManager.loadVideoTrack(selectedVideoTrack)
        }
        playerViewModel.showTrackMenu()
        rvVideoTracks.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.VIDEO_TRACKS)
    }

    private fun loadSourcesMenu() {
        val isAutoSelected = playerViewModel.isSourceForced.value == false
        val currentSource = playerViewModel.currentStreamSource.value
        val currentSourceLabel = currentSource?.let { source ->
            val currentName = source.name?.takeIf { it.isNotBlank() } ?: "Sin nombre"
            "${source.index} - $currentName"
        }.orEmpty()
        val autoSubtitle = if (isAutoSelected && currentSourceLabel.isNotBlank()) "Usando: $currentSourceLabel" else ""
        
        val sourcesList = mutableListOf(
            StreamSourceItem(id = -1, name = getString(R.string.auto), url = autoSubtitle, apiCalls = listOf(),
                headers = listOf(), index = -1, streamSourceType = StreamSourceTypeItem.IPTV,
                isSelected = isAutoSelected, drmType = DrmTypeItem.NONE)
        )

        sourcesList += playerViewModel.currentChannel.value?.streamSources!!.sortedBy { it.index }
        for (i in sourcesList.indices) {
            if (i != 0) {
                sourcesList[i].isSelected = playerViewModel.isSourceForced.value == true && 
                    playerViewModel.currentStreamSource.value?.id == sourcesList[i].id
            }
        }
        rvChannelSources.adapter = ChannelSourcesAdapter(sourcesList) { selectedSource ->
            playerViewModel.updateIsSourceForced(selectedSource.id.toInt() != -1)
            loadStreamSource(selectedSource)
        }

        playerViewModel.showTrackMenu()
        rvChannelSources.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.SOURCES)
    }

    private fun initCategoryList() {
        lifecycleScope.launch {
            val categories = listOf(CategoryItem(-1L, "Favoritos", null, false, listOf())) + channelViewModel.getCategories()
            rvCategoryList.adapter = CategoryListAdapter(categories) { selectedCategory ->
                channelViewModel.updateIsLoadingChannel(true)
                playerViewModel.updateCurrentCategoryId(selectedCategory.id)
                playerViewModel.updateCategoryName(selectedCategory.name)
                lifecycleScope.launch {
                    loadChannel(channelViewModel.getNextChannel(categoryId = selectedCategory.id, groupId = 1))
                    channelViewModel.updateIsLoadingChannel(false)
                    channelViewModel.updateLastCategoryLoaded(selectedCategory.id)
                }
                uiController.hideChannelInfo()
                uiController.hideMediaAndBottomInfo()
                playerViewModel.hideCategoryList()
            }
        }
    }

    private suspend fun initChannelList() {
        val sortedChannels = channelViewModel.getSmChannelsByCategory(playerViewModel.currentCategoryId.value!!)
        val currentIndex = sortedChannels.indexOfFirst { it.id == playerViewModel.currentChannel.value?.id }
        if (currentIndex != -1) playerViewModel.updateCurrentItemSelectedFromChannelList(currentIndex)
        rvChannelList.adapter = ChannelListAdapter(playerViewModel.currentCategoryId.value!!, sortedChannels) { loadChannel(it) }
    }

    private fun initNumberList(){
        rvNumberList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        rvNumberList.adapter = NumberListAdapter(numbers) { selectedNumber ->
            uiController.jobUIChangeChannel?.cancel()
            uiController.hideChannelInfo()
            if (playerViewModel.getCurrentNumberInput().length >= MAX_DIGITS) playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().append(selectedNumber))
            binding.channelNumberKeyboard.text = playerViewModel.getCurrentNumberInput().toString()
            uiController.showChannelNumberWithTimeoutAndChangeChannel { loadChannel(it) }
        }
    }

    private fun loadChannel(channel: ChannelItem) {
        if (::jobLoadStreamSource.isInitialized && jobLoadStreamSource.isActive) jobLoadStreamSource.cancel()
        uiController.jobUIChangeChannel?.cancel()
        uiController.jobEPGRender?.cancel()
        if (::jobLoadChannel.isInitialized && jobLoadChannel.isActive) jobLoadChannel.cancel()

        jobLoadChannel = lifecycleScope.launch {
            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
            playerViewModel.hidePlayer()

            if (channel.id < 0) {
                playerViewModel.hideChannelNumber()
                playerViewModel.hideChannelName()
                return@launch
            }
            uiController.hideMediaAndBottomInfo()
            playerViewModel.hideNumberListMenu()

            timerManager.cancelSourceLoadingTimer()
            timerManager.cancelBufferingTimer()
            timerManager.cancelCheckPlayingCorrectlyTimer()
            timerManager.cancelLoadingIndicatorTimer()
            timerManager.startLoadingIndicatorTimer {
                if (playerViewModel.isAnimatedLoadingIconVisible.value == false) {
                    if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                    playerViewModel.showAnimatedLoadingIcon()
                }
            }

            playerViewModel.updateIsQualityForced(false)

            if (player.isPlaying || player.isLoading) player.stop()

            playerViewModel.hideAnimatedLoadingIcon()
            playerViewModel.hideErrorMessage()
            playerViewModel.hideBottomErrorMessage()

            playerViewModel.updateChannelName(channel.name)
            val index = if (playerViewModel.currentCategoryId.value == -1L) channel.indexFavourite else channel.indexGroup
            index?.let {
                playerViewModel.updateChannelNumber(it)
                playerViewModel.updateChannelIdFastSwitch(it)
            }

            channelViewModel.updateCurrentProgram(null)
            channelViewModel.updateNextProgram(null)
            playerViewModel.updateIsSourceForced(false)
            playerViewModel.updateCurrentChannel(channel)
            uiController.showChannelInfoWithTimeout()

            channelViewModel.updateLastChannelLoaded(channel.id)
            channelViewModel.updateLastCategoryLoaded(playerViewModel.currentCategoryId.value ?: -1L)

            if (channel.streamSources.isNotEmpty()) {
                loadStreamSource(channel.streamSources.minBy { it.index })
                playerViewModel.updateTriesCountForEachSource(0)
                playerViewModel.updateSourcesTriedCount(0)
            }
        }
    }

    private fun loadStreamSource(streamSource: StreamSourceItem) {
        if (::jobLoadStreamSource.isInitialized && jobLoadStreamSource.isActive) jobLoadStreamSource.cancel()
        jobLoadStreamSource = lifecycleScope.launch {
            timerManager.cancelSourceLoadingTimer()
            timerManager.cancelBufferingTimer()
            timerManager.cancelCheckPlayingCorrectlyTimer()

            if (playerViewModel.currentStreamSource.value?.id != streamSource.id) {
                playerViewModel.updateIsQualityForced(false)
                playerViewModel.hidePlayer()
            }

            if (player.isLoading || player.isPlaying) player.stop()

            if (playerViewModel.isSourceForced.value == true && playerViewModel.isChannelLoading.value == false){
                timerManager.cancelLoadingIndicatorTimer()
                timerManager.startLoadingIndicatorTimer {
                    if (playerViewModel.isAnimatedLoadingIconVisible.value == false) {
                        if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                        playerViewModel.showAnimatedLoadingIcon()
                    }
                }
            }

            playerViewModel.setIsSourceLoading(true)
            timerManager.startSourceLoadingTimer {
                if (playerViewModel.isSourceLoading.value == true) {
                    playerViewModel.setIsSourceLoading(false)
                    val currentChannel = playerViewModel.currentChannel.value
                    val currentStreamSource = playerViewModel.currentStreamSource.value
                    if (currentChannel != null && currentStreamSource != null) {
                        lifecycleScope.launch { streamSourceManager.tryNextStreamSource(currentChannel, currentStreamSource) }
                    }
                }
            }
            playerViewModel.hideMediaInfo()
            resetMediaInfo()
            playerViewModel.updateCurrentStreamSource(streamSource)
            delay(250L)
            streamSourceManager.loadStreamSource(streamSource)
        }
    }

    private fun resetMediaInfo() {
        mediaInfo.videoResolution = null
        mediaInfo.videoQuality = null
        mediaInfo.videoAspectRatio = null
        mediaInfo.videoCodec = null
        mediaInfo.videoFrameRate = null
        mediaInfo.videoBitrate = null
        mediaInfo.audioBitrate = null
        mediaInfo.audioSamplingRate = null
        mediaInfo.audioChannels = null
        mediaInfo.audioCodec = null
        mediaInfo.hasSubtitles = false
        mediaInfo.hasEPG = false
        mediaInfo.hasTeletext = false
        mediaInfo.hasDRM = false
        mediaInfo.hasMultipleAudios = false
        playerViewModel.updateMediaInfo(mediaInfo)
    }

    @SuppressLint("RestrictedApi")
    fun handleKeyEvent(event: KeyEvent): Boolean = keyHandler.handle(event)

    private fun loadPreviousChannel(){
        val currentChannelIndex = if (playerViewModel.currentCategoryId.value == -1L) {
            playerViewModel.currentChannel.value?.indexFavourite ?: 0
        } else {
            playerViewModel.currentChannel.value?.indexGroup ?: 0
        }
        jobSwitchChannel?.cancel()
        jobSwitchChannel = lifecycleScope.launch {
            channelViewModel.updateIsLoadingChannel(true)
            val channel = channelViewModel.getPreviousChannel(playerViewModel.currentCategoryId.value!!, currentChannelIndex - 1)
            channelViewModel.updateIsLoadingChannel(false)
            loadChannel(channel)
        }
    }

    private fun loadNextChannel(){
        val currentChannelIndex = if (playerViewModel.currentCategoryId.value == -1L) {
            playerViewModel.currentChannel.value?.indexFavourite ?: 0
        } else {
            playerViewModel.currentChannel.value?.indexGroup ?: 0
        }
        jobSwitchChannel?.cancel()
        jobSwitchChannel = lifecycleScope.launch {
            channelViewModel.updateIsLoadingChannel(true)
            val channel = channelViewModel.getNextChannel(playerViewModel.currentCategoryId.value!!, currentChannelIndex + 1)
            channelViewModel.updateIsLoadingChannel(false)
            loadChannel(channel)
        }
    }

    override fun onStart() {
        super.onStart()
        channelViewModel.startEpgAutoRefresh()
        player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        if (isInEPGPictureInPictureMode) {
            player.playWhenReady = true
            uiController.hideStandardButtons()
            playerViewModel.apply {
                hideChannelList(); hideSettingsMenu(); hideTrackMenu()
                hideMediaInfo(); hideChannelName(); hideChannelNumber()
                hideChannelNumberKeyboard(); hideTimeDate(); hideBottomInfo()
            }
        } else {
            player.playWhenReady = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (playerViewModel.currentStreamSource.value != null && !activity?.isInPictureInPictureMode!!) {
            loadStreamSource(playerViewModel.currentStreamSource.value!!)
        }
        player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        channelViewModel.stopEpgAutoRefresh()
        if (!isInEPGPictureInPictureMode) player.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPiPModeIfSupported() {
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        requireActivity().enterPictureInPictureMode(pipParams)
    }

    companion object {
        val TAG: String = PlayerFragment::class.java.name
    }
}

fun View.bindVisibility(lifecycleOwner: LifecycleOwner, liveData: LiveData<Boolean>, invisibleInsteadOfGone: Boolean = false) {
    liveData.observe(lifecycleOwner) { isVisible ->
        visibility = if (isVisible) View.VISIBLE else (if (invisibleInsteadOfGone) View.INVISIBLE else View.GONE)
    }
}

fun orLiveData(a: LiveData<Boolean>, b: LiveData<Boolean>): LiveData<Boolean> {
    return MediatorLiveData<Boolean>().apply {
        val observer = Observer<Any> { value = a.value == true || b.value == true }
        addSource(a, observer); addSource(b, observer)
    }
}
