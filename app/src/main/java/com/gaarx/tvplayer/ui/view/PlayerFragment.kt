package com.gaarx.tvplayer.ui.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import androidx.media3.common.C
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
import com.gaarx.tvplayer.domain.model.AudioTrack
import com.gaarx.tvplayer.domain.model.CategoryItem
import com.gaarx.tvplayer.domain.model.ChannelItem
import com.gaarx.tvplayer.domain.model.ChannelSettings
import com.gaarx.tvplayer.domain.model.DrmTypeItem
import com.gaarx.tvplayer.domain.model.MediaInfo
import com.gaarx.tvplayer.domain.model.StreamSourceItem
import com.gaarx.tvplayer.domain.model.StreamSourceTypeItem
import com.gaarx.tvplayer.domain.model.SubtitlesTrack
import com.gaarx.tvplayer.domain.model.VideoTrack
import com.gaarx.tvplayer.exceptions.ChannelNotFoundException
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import kotlinx.coroutines.flow.collectLatest
import com.gaarx.tvplayer.core.Constants.DEFAULT_REFRESH_RATE
import com.gaarx.tvplayer.core.Constants.MAX_DIGITS
import com.gaarx.tvplayer.core.Constants.TIMEOUT_UI_CHANNEL_LOAD
import com.gaarx.tvplayer.ui.util.PlayerLifecycleManager
import com.gaarx.tvplayer.util.DeviceUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.reflect.KMutableProperty0

@UnstableApi
@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var animatorSet: AnimatorSet

    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector

    private var mediaInfo: MediaInfo = MediaInfo()

    private var jobUITimeout: Job? = null
    private lateinit var jobLoadStreamSource: Job
    private lateinit var jobLoadChannel: Job
    private var jobUIChangeChannel: Job? = null
    private lateinit var jobEPGRender : Job
    private lateinit var jobSwitchChannel : Job

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

    //private var originalProxySelector: ProxySelector? = null

    private var isInEPGPictureInPictureMode: Boolean = false

    private var timerManager = PlayerTimerManager()
    private lateinit var lifecycleManager: PlayerLifecycleManager
    private lateinit var playerManager: PlayerManager
    private lateinit var streamSourceManager: StreamSourceManager
    private lateinit var keyHandler: KeyEventHandler

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
        //originalProxySelector = ProxySelector.getDefault()
        lifecycleManager = PlayerLifecycleManager(
            timerManager
        )
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
            }
            else{
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
                    Log.i(
                        "PlayerActivity",
                        "lastChannelId: $lastChannelId. lastCategoryId: $lastCategoryId"
                    )

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
                        println("lastcategoryid: $lastCategoryId")
                        if (channel != null) {
                            playerViewModel.updateCurrentCategoryId(lastCategoryId)
                            val category = channelViewModel.getCategoryById(lastCategoryId)
                            Log.i("PlayerActivity", "category: $category")
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
            onShowChannelInfoWithTimeout = { showChannelInfoWithTimeout() },
            onShowFullChannelUIWithTimeout = { timeout -> showFullChannelUIWithTimeout(timeout) },
            onShowChannelNumberWithTimeoutAndChangeChannel = { showChannelNumberWithTimeoutAndChangeChannel() },
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
                        val audioTrackList = loadAudioTracks()
                        val audioTrackLoader = playerManager.getAudioTrackLoader()

                        rvAudioTracks.adapter = AudioTracksAdapter(audioTrackList, audioTrackLoader)
                        rvAudioTracks.requestFocus()
                    }

                    ChannelSettings.SUBTITLES_TRACKS -> {
                        val subtitlesTrackList = loadSubtitlesTracks()
                        val subtitlesTrackLoader = playerManager.getSubtitlesTrackLoader()

                        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(subtitlesTrackList, subtitlesTrackLoader)
                        rvSubtitlesTracks.requestFocus()
                    }

                    ChannelSettings.VIDEO_TRACKS -> {
                        val videoTrackList = loadVideoTracks()
                        val videoTrackLoader = playerManager.getVideoTrackLoader()
                        val isQualityForced = playerViewModel.isQualityForced.value == true

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
        setupRecyclerViews()
        lifecycleScope.launch {
            val lastDownloadedTime = channelViewModel.getEPGLastDownloadedTime()
            println("lastDownloadedTime: $lastDownloadedTime, current: "+ (System.currentTimeMillis() - lastDownloadedTime))
            if (lastDownloadedTime <= 0L || System.currentTimeMillis() - lastDownloadedTime > 2 * 60 * 60 * 1000) {
                channelViewModel.downloadEPG()
            }
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
            setupObservers()
            setupClickListeners()
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
            if (mediaInfo.videoResolution != null) {
                binding.channelVideoResolution.visibility = View.VISIBLE
                binding.channelVideoResolution.text = mediaInfo.videoResolution
            }
            else{
                binding.channelVideoResolution.visibility = View.GONE
            }
            if (mediaInfo.videoAspectRatio != null) {
                binding.channelVideoAspectRatio.visibility = View.VISIBLE
                binding.channelVideoAspectRatio.text = mediaInfo.videoAspectRatio
            }
            else{
                binding.channelVideoAspectRatio.visibility = View.GONE
            }
            if (mediaInfo.videoQuality != null) {
                binding.channelVideoQuality.visibility = View.VISIBLE
                binding.channelVideoQuality.text = mediaInfo.videoQuality
            }
            else{
                binding.channelVideoQuality.visibility = View.GONE
            }
            if (mediaInfo.videoCodec != null) {
                binding.channelVideoCodec.visibility = View.VISIBLE
                binding.channelVideoCodec.text = mediaInfo.videoCodec
            }
            else{
                binding.channelVideoCodec.visibility = View.GONE
            }
            // Video bitrate values are only those defined in the manifest
            if (mediaInfo.videoBitrate != null) {
                binding.channelVideoBitrate.visibility = View.VISIBLE
                binding.channelVideoBitrate.text = mediaInfo.videoBitrate
            }
            else{
                binding.channelVideoBitrate.visibility = View.GONE
            }
            if (mediaInfo.videoFrameRate != null) {
                binding.channelVideoFrameRate.visibility = View.VISIBLE
                binding.channelVideoFrameRate.text = mediaInfo.videoFrameRate
            }
            else{
                binding.channelVideoFrameRate.visibility = View.GONE
            }
            if (mediaInfo.audioCodec != null) {
                binding.channelAudioCodec.visibility = View.VISIBLE
                binding.channelAudioCodec.text = mediaInfo.audioCodec
            }
            else{
                binding.channelAudioCodec.visibility = View.GONE
            }
            // Values are sometimes inaccurate
            if (mediaInfo.audioBitrate != null) {
                binding.channelAudioBitrate.visibility = View.VISIBLE
                binding.channelAudioBitrate.text = mediaInfo.audioBitrate
            }
            else{
                binding.channelAudioBitrate.visibility = View.GONE
            }
            if (mediaInfo.audioSamplingRate != null) {
                binding.channelAudioSamplingRate.visibility = View.VISIBLE
                binding.channelAudioSamplingRate.text = mediaInfo.audioSamplingRate
            }
            else{
                binding.channelAudioSamplingRate.visibility = View.GONE
            }
            if (mediaInfo.audioChannels != null) {
                binding.channelAudioChannels.visibility = View.VISIBLE
                binding.channelAudioChannels.text = mediaInfo.audioChannels
            }
            else{
                binding.channelAudioChannels.visibility = View.GONE
            }
            if (mediaInfo.hasSubtitles) {
                binding.channelHasSubtitles.visibility = View.VISIBLE
            }
            else{
                binding.channelHasSubtitles.visibility = View.GONE
            }
            if (mediaInfo.hasEPG) {
                binding.channelHasEPG.visibility = View.VISIBLE
            }
            else{
                binding.channelHasEPG.visibility = View.GONE
            }
            if (mediaInfo.hasMultipleAudios) {
                binding.channelHasMultiLanguageAudio.visibility = View.VISIBLE
            }
            else{
                binding.channelHasMultiLanguageAudio.visibility = View.GONE
            }

            if (mediaInfo.hasAudioDescription) {
                binding.channelHasAudioDescription.visibility = View.VISIBLE
            }
            else {
                binding.channelHasAudioDescription.visibility = View.GONE
            }

            if (mediaInfo.hasTeletext) {
                binding.channelHasTeletext.visibility = View.VISIBLE
            }
            else{
                binding.channelHasTeletext.visibility = View.GONE
            }

            if (mediaInfo.hasDRM) {
                binding.channelHasDRM.visibility = View.VISIBLE
            }
            else{
                binding.channelHasDRM.visibility = View.GONE
            }
        }

        playerViewModel.bottomErrorMessage.observe(viewLifecycleOwner) { bottomErrorMessage ->
            binding.bottomErrorMessage.text = bottomErrorMessage
        }

        playerViewModel.isAnimatedLoadingIconVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) showLoadingDots() else hideLoadingDots()
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
        binding.message.bindVisibility(viewLifecycleOwner, playerViewModel.isErrorMessageVisible)
        binding.rvNumberList.bindVisibility(viewLifecycleOwner, playerViewModel.isNumberListMenuVisible)
        binding.channelList.bindVisibility(viewLifecycleOwner, playerViewModel.isChannelListVisible)
        binding.rvChannelSettings.bindVisibility(viewLifecycleOwner, playerViewModel.isSettingsMenuVisible)
        binding.rvChannelTrackSettings.bindVisibility(viewLifecycleOwner, playerViewModel.isTrackMenuVisible)

        channelViewModel.currentProgram.observe(viewLifecycleOwner) { currentProgram ->
            if (currentProgram != null){
                val currentProgramStartTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentProgram.startTime)
                val currentProgramStopTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentProgram.stopTime)
                val currentTime = System.currentTimeMillis()
                val currentProgramDuration = currentProgram.stopTime.time.minus(
                    currentProgram.startTime.time
                )
                val progress =
                    (currentTime - currentProgram.startTime.time) * 100 / currentProgramDuration
                val ageRatingIcon = currentProgram.ageRatingIcon
                val ageRatingText = currentProgram.ageRating
                if (!ageRatingIcon.isNullOrBlank()) {
                    binding.ivAgeRating.visibility = View.VISIBLE
                } else {
                    if (ageRatingText != null) {
                        binding.tvAgeRating.text = ageRatingText
                        binding.tvAgeRating.visibility = View.VISIBLE
                    }
                    else{
                        binding.tvAgeRating.visibility = View.GONE
                    }
                    binding.ivAgeRating.visibility = View.GONE
                }
                binding.progressBar.progress = progress.toInt()
                binding.tvChannelCurrentProgram.text = currentProgram.title
                binding.tvChannelCurrentProgram.visibility = View.VISIBLE
                binding.progressBar.visibility = View.VISIBLE
                binding.tvStartTime.visibility = View.VISIBLE
                binding.tvEndTime.visibility = View.VISIBLE
                binding.tvStartTime.text = currentProgramStartTimeFormatted
                binding.tvEndTime.text = currentProgramStopTimeFormatted
                // Load image into ImageView
                Glide.with(this)
                    .load(ageRatingIcon)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original and resized versions
                    .into(binding.ivAgeRating) // Target ImageView
                if (playerViewModel.isChannelNameVisible.value == true){
                    binding.channelBottomInfo.visibility = View.VISIBLE
                }
            } else{
                if (channelViewModel.nextProgram.value == null){
                    println("viewbottominfo gone")
                    binding.channelBottomInfo.visibility = View.GONE
                    mediaInfo.hasEPG = false
                    playerViewModel.updateMediaInfo(mediaInfo)
                }
                binding.progressBar.visibility = View.GONE
                binding.tvStartTime.visibility = View.GONE
                binding.tvEndTime.visibility = View.GONE
                binding.tvChannelCurrentProgram.visibility = View.GONE
                binding.ivAgeRating.visibility = View.GONE
                binding.tvAgeRating.visibility = View.GONE
            }
        }

        channelViewModel.nextProgram.observe(viewLifecycleOwner) { nextProgram ->
            if (nextProgram != null){
                val nextProgramStartTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextProgram.startTime)
                val nextProgramStopTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextProgram.stopTime)
                binding.tvChannelNextProgram.text = getString(R.string.nextProgram) + nextProgram.title + " (" +  nextProgramStartTimeFormatted + " - " + nextProgramStopTimeFormatted + ")"
                binding.tvChannelNextProgram.visibility = View.VISIBLE
                if (channelViewModel.currentProgram.value == null && playerViewModel.isChannelNameVisible.value == true){
                    binding.channelBottomInfo.visibility = View.VISIBLE
                }
            }
            else{
                if (channelViewModel.currentProgram.value == null){
                    binding.channelBottomInfo.visibility = View.GONE
                    mediaInfo.hasEPG = false
                    playerViewModel.updateMediaInfo(mediaInfo)
                }
                binding.tvChannelNextProgram.visibility = View.GONE
            }
        }

        channelViewModel.currentProgram.observe(viewLifecycleOwner) { currentProgram ->
            if (currentProgram != null){
                mediaInfo.hasEPG = true
                playerViewModel.updateMediaInfo(mediaInfo)
            }
            else if (channelViewModel.nextProgram.value != null){
                mediaInfo.hasEPG = true
                playerViewModel.updateMediaInfo(mediaInfo)
            }
            else{
                mediaInfo.hasEPG = false
                playerViewModel.updateMediaInfo(mediaInfo)
            }
        }

        playerViewModel.isCategoryListVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                binding.rvCategoryList.visibility = View.VISIBLE
            }
            else{
                binding.rvCategoryList.visibility = View.GONE
            }
        }

    }

    private fun setupClickListeners(){
        binding.playerView.setOnClickListener {
            if (playerViewModel.isSettingsMenuVisible.value == true) {
                playerViewModel.hideSettingsMenu()
            }
            else if (playerViewModel.isChannelListVisible.value == true) {
                playerViewModel.hideChannelList()
            }
            else if (playerViewModel.isTrackMenuVisible.value == true) {
                playerViewModel.hideTrackMenu()
            }
            else if (playerViewModel.isChannelNumberVisible.value == true) {
                if (!DeviceUtil.isAndroidTV(requireContext())) {
                    playerViewModel.hideButtonUp()
                    playerViewModel.hideButtonDown()
                    playerViewModel.hideButtonChannelList()
                    playerViewModel.hideButtonSettings()
                    playerViewModel.hideButtonPiP()
                    playerViewModel.hideButtonCategoryList()
                }
                playerViewModel.hideChannelName()
                playerViewModel.hideChannelNumber()
                playerViewModel.hideCategoryName()
                playerViewModel.hideTimeDate()
                playerViewModel.hideMediaInfo()
                playerViewModel.hideBottomInfo()
            }
            else if (playerViewModel.isCategoryListVisible.value == true) {
                playerViewModel.hideCategoryList()
            }
            else{
                showFullChannelUIWithTimeout()
            }
        }

        binding.buttonUp.setOnClickListener {
            loadNextChannel()
        }

        binding.buttonDown.setOnClickListener {
            loadPreviousChannel()
        }

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
                    //initFocusInChannelList()
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
            ChannelSettings.AUDIO_TRACKS -> {
                loadAudioTracksMenu()
            }
            ChannelSettings.SUBTITLES_TRACKS -> {
                loadSubtitlesTracksMenu()
            }
            ChannelSettings.VIDEO_TRACKS -> {
                loadVideoTracksMenu()
            }
            ChannelSettings.SOURCES -> {
                loadSourcesMenu()
            }
            ChannelSettings.UPDATE_EPG -> {
                channelViewModel.updateEPG()
            }
            ChannelSettings.SHOW_EPG -> {
                val mainActivity = requireActivity() as MainActivity
                mainActivity.showPipOverlay(player) // Reuses the ExoPlayer instance
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
                    try{
                        val firstChannel = channelViewModel.getChannel(-1L, 1)
                        loadChannel(firstChannel)
                    }
                    catch (e: Exception){
                        Toast.makeText(requireContext(), "Error al cargar la lista de canales: $e", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ChannelSettings.ASPECT_RATIO -> {
                loadAspectRatioMenu()
            }
            ChannelSettings.CONFIG_URL -> {
                lifecycleScope.launch {
                    val url = loadConfigURLDialogSuspend()
                    if (!url.isNullOrEmpty()) {
                        channelViewModel.updateConfigURL(url)
                        Toast.makeText(requireContext(), "Config URL updated: $url", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Config URL not set", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadAspectRatioMenu() {
        val aspectRatios = listOf("Auto", "Fill", "Zoom")
        val aspectRatioValues = listOf(AspectRatio.AUTO, AspectRatio.FILL, AspectRatio.ZOOM)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Selecciona relación de aspecto")
        builder.setItems(aspectRatios.toTypedArray()) { _, which ->
            applyAspectRatio(aspectRatioValues[which])
        }
        builder.show()
    }

    private fun applyAspectRatio(aspectRatio: AspectRatio) {
        when (aspectRatio) {
            AspectRatio.AUTO -> {
                binding.playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            AspectRatio.FILL -> {
                binding.playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            AspectRatio.ZOOM -> {
                binding.playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        }
    }

    enum class AspectRatio {
        AUTO, FILL, ZOOM
    }

    private suspend fun loadConfigURLDialogSuspend(): String? =
        suspendCancellableCoroutine { cont ->
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
                        Toast.makeText(requireContext(), getString(R.string.dialog_config_url_toast_accepted) + " $url", Toast.LENGTH_SHORT).show()
                        cont.resume(url) { _, _, _ -> dialogInterface.dismiss() }
                    } else {
                        Toast.makeText(requireContext(), "URL cannot be empty", Toast.LENGTH_SHORT).show()
                        cont.resume(null) { _, _, _ -> dialogInterface.dismiss() }
                    }
                }
                .setNegativeButton(getString(R.string.dialog_config_url_cancel)) { dialogInterface, _ ->
                    cont.resume(null) { _, _, _ -> dialogInterface.dismiss() }
                }
                .create()

            dialog.setOnCancelListener {
                cont.resume(null) { _, _, _ -> dialog.dismiss() }
            }

            cont.invokeOnCancellation {
                if (dialog.isShowing) dialog.dismiss()
            }

            dialog.show()
        }

    private fun loadAudioTracksMenu(){
        val audioTrackList = loadAudioTracks()
        rvAudioTracks.adapter = AudioTracksAdapter(audioTrackList) { selectedAudioTrack ->
            playerManager.loadAudioTrack(selectedAudioTrack)
        }
        playerViewModel.showTrackMenu()
        rvAudioTracks.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.AUDIO_TRACKS)
    }

    private fun loadSubtitlesTracksMenu() {
        val subtitlesTrackList = loadSubtitlesTracks()
        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(subtitlesTrackList) { selectedSubtitlesTrack ->
            playerManager.loadSubtitlesTrack(selectedSubtitlesTrack)
        }
        playerViewModel.showTrackMenu()
        rvSubtitlesTracks.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.SUBTITLES_TRACKS)
    }

    private fun loadVideoTracksMenu() {
        val videoTracksList = loadVideoTracks()
        val isQualityForced = playerViewModel.isQualityForced.value!!
        rvVideoTracks.adapter = VideoTracksAdapter(isQualityForced, videoTracksList) { selectedVideoTrack ->
            if (selectedVideoTrack.id.toInt() == -1) {
                playerViewModel.updateIsQualityForced(false)
            }
            else{
                playerViewModel.updateIsQualityForced(true)
            }
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
            source.index.toString() + " - " + currentName
        }.orEmpty()
        val autoSubtitle = if (isAutoSelected && currentSourceLabel.isNotBlank()) {
            "Usando: " + currentSourceLabel
        } else {
            ""
        }
        val sourcesList = mutableListOf(
            StreamSourceItem(id = -1,
                name = getString(R.string.auto),
                url = autoSubtitle,
                apiCalls = listOf(),
                headers = listOf(),
                index = -1,
                streamSourceType = StreamSourceTypeItem.IPTV,
                isSelected = isAutoSelected,
                drmType = DrmTypeItem.NONE
            )
        )

        sourcesList += playerViewModel.currentChannel.value?.streamSources!!.sortedBy { it.index }
        for (i in sourcesList.indices) {
            if (i != 0) {
                if (playerViewModel.isSourceForced.value == true) {
                    sourcesList[i].isSelected = playerViewModel.currentStreamSource.value!!.id == sourcesList[i].id
                }
                else{
                    sourcesList[i].isSelected = false
                }
            }
        }
        rvChannelSources.adapter = ChannelSourcesAdapter(sourcesList) { selectedSource ->
            if (selectedSource.id.toInt() == -1) {
                playerViewModel.updateIsSourceForced(false)
            }
            else{
                playerViewModel.updateIsSourceForced(true)
            }
            loadStreamSource(selectedSource)
        }

        playerViewModel.showTrackMenu()
        rvChannelSources.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.SOURCES)
    }

    private fun loadAudioTracks(): List<AudioTrack> =
        loadTracks(C.TRACK_TYPE_AUDIO) { format, group, index ->
            AudioTrack(
                id = format.id.orEmpty(),
                language = format.language.orEmpty(),
                codec = format.codecs ?: format.sampleMimeType.orEmpty(),
                bitrate = format.bitrate,
                channelCount = format.channelCount,
                isSelected = group.isTrackSelected(index)
            )
        }

    private fun loadSubtitlesTracks(): List<SubtitlesTrack> {
        val offTrack = SubtitlesTrack("-1", getString(R.string.off), "", true)
        val list = loadTracks(C.TRACK_TYPE_TEXT, offTrack) { format, group, index ->
            if (group.isTrackSelected(index)) offTrack.isSelected = false
            SubtitlesTrack(
                id = format.id.orEmpty(),
                language = format.language.orEmpty(),
                codec = format.codecs ?: format.sampleMimeType.orEmpty(),
                isSelected = group.isTrackSelected(index)
            )
        }
        return list
    }

    private fun loadVideoTracks(): List<VideoTrack> =
        loadTracks(
            C.TRACK_TYPE_VIDEO,
            VideoTrack(
                "-1",
                getString(R.string.auto),
                -1, -1, 0, "",
                playerViewModel.isQualityForced.value == false
            )
        ) { format, group, index ->
            val isSelected = if (playerViewModel.isQualityForced.value == false) {
                false
            } else {
                group.isTrackSelected(index)
            }
            VideoTrack(
                id = format.id.orEmpty(),
                name = format.codecs.orEmpty(),
                width = format.width,
                height = format.height,
                bitrate = format.bitrate / 1000,
                codec = format.codecs ?: format.sampleMimeType.orEmpty(),
                isSelected = isSelected
            )
        }

    private fun <T> loadTracks(
        trackType: Int,
        defaultItem: T? = null,
        mapper: (format: Format, group: Tracks.Group, index: Int) -> T
    ): List<T> {
        val result = mutableListOf<T>()
        defaultItem?.let { result += it }

        val currentTracks = player.currentTracks
        for (group in currentTracks.groups) {
            if (group.type == trackType) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    result += mapper(format, group, i)
                }
            }
        }
        return result
    }

    private fun initCategoryList() {
        lifecycleScope.launch {
            val categories = listOf(CategoryItem(-1L, "Favoritos", null, false, listOf())) + channelViewModel.getCategories()
            Log.i("PlayerActivity", "initCategoryList: ${categories.size}")
            rvCategoryList.adapter = CategoryListAdapter(categories) { selectedCategory ->
                channelViewModel.updateIsLoadingChannel(true)
                playerViewModel.updateCurrentCategoryId(selectedCategory.id)
                playerViewModel.updateCategoryName(selectedCategory.name)
                lifecycleScope.launch {
                    loadChannel(channelViewModel.getNextChannel(categoryId = selectedCategory.id, groupId = 1))
                    channelViewModel.updateIsLoadingChannel(false)
                    Log.i(TAG, "dispatchKeyEvent: newCategory: $selectedCategory")
                    channelViewModel.updateLastCategoryLoaded(selectedCategory.id)
                }

                playerViewModel.hideChannelName()
                playerViewModel.hideCategoryName()
                playerViewModel.hideChannelNumber()
                playerViewModel.hideBottomInfo()
                playerViewModel.hideMediaInfo()
                playerViewModel.hideTimeDate()

                playerViewModel.hideCategoryList()
            }
        }
    }

    private suspend fun initChannelList() {
        val sortedChannels = channelViewModel.getSmChannelsByCategory(playerViewModel.currentCategoryId.value!!)
        for (i in sortedChannels.indices) {
            if (sortedChannels[i].id == playerViewModel.currentChannel.value?.id) {
                playerViewModel.updateCurrentItemSelectedFromChannelList(i)
                break
            }
        }
        rvChannelList.adapter = ChannelListAdapter(playerViewModel.currentCategoryId.value!!, sortedChannels) { selectedChannel ->
            loadChannel(selectedChannel)
        }
    }

    private fun initNumberList(){
        rvNumberList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        rvNumberList.adapter = NumberListAdapter(numbers) { selectedNumber ->
            if (jobUIChangeChannel?.isActive == true) {
                jobUIChangeChannel?.cancel()
            }
            if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
            if (playerViewModel.isChannelNameVisible.value == true) playerViewModel.hideChannelName()
            if (playerViewModel.isChannelNumberVisible.value == true) playerViewModel.hideChannelNumber()
            if (playerViewModel.isCategoryNameVisible.value == true) playerViewModel.hideCategoryName()
            if (playerViewModel.getCurrentNumberInput().length >= MAX_DIGITS) {
                playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
            }

            val number = (selectedNumber).toString()

            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().append(number))

            binding.channelNumberKeyboard.text = playerViewModel.getCurrentNumberInput().toString()

            showChannelNumberWithTimeoutAndChangeChannel()
        }

    }

    private fun loadChannel(channel: ChannelItem) {
        println("loadChannel")
        if (::jobLoadStreamSource.isInitialized && (jobLoadStreamSource.isActive)) {
            jobLoadStreamSource.cancel()
        }
        if (jobUIChangeChannel?.isActive == true) {
            jobUIChangeChannel?.cancel()
        }
        if (::jobEPGRender.isInitialized && jobEPGRender.isActive) {
            jobEPGRender.cancel()
        }
        if (::jobLoadChannel.isInitialized && jobLoadChannel.isActive) {
            jobLoadChannel.cancel()
        }
        jobLoadChannel = lifecycleScope.launch {
            playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
            playerViewModel.hidePlayer()

            if (channel.id < 0) {
                if (binding.channelNumber.isVisible) playerViewModel.hideChannelNumber()
                if (binding.channelName.isVisible) playerViewModel.hideChannelName()
                return@launch
            }
            playerViewModel.hideBottomInfo()
            playerViewModel.hideNumberListMenu()

            if (playerViewModel.isSourceLoading.value == true) timerManager.cancelSourceLoadingTimer()
            if (playerViewModel.isBuffering.value == true) timerManager.cancelBufferingTimer()
            timerManager.cancelCheckPlayingCorrectlyTimer()
            timerManager.cancelLoadingIndicatorTimer()
            timerManager.startLoadingIndicatorTimer{
                if (playerViewModel.isAnimatedLoadingIconVisible.value == false) {
                    Log.i(TAG, "Channel Loading Timeout")
                    if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                    playerViewModel.showAnimatedLoadingIcon()
                }
            }

            if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
            playerViewModel.updateIsQualityForced(false)

            if (player.isPlaying || player.isLoading){
                player.stop()
            }

            if (binding.loadingDots.isVisible) playerViewModel.hideAnimatedLoadingIcon()
            if (binding.message.isVisible) playerViewModel.hideErrorMessage()
            playerViewModel.hideBottomErrorMessage()
            Log.i(TAG,channel.name)

            playerViewModel.updateChannelName(channel.name)
            if (playerViewModel.currentCategoryId.value == -1L) {
                playerViewModel.updateChannelNumber(channel.indexFavourite!!)
                playerViewModel.updateChannelIdFastSwitch(channel.indexFavourite)
            }
            else {
                playerViewModel.updateChannelNumber(channel.indexGroup!!)
                playerViewModel.updateChannelIdFastSwitch(channel.indexGroup)
            }

            channelViewModel.updateCurrentProgram(null)
            channelViewModel.updateNextProgram(null)

            playerViewModel.updateIsSourceForced(false)
            playerViewModel.updateCurrentChannel(channel)
            showChannelInfoWithTimeout()
            lifecycleScope.launch {
                channelViewModel.updateLastChannelLoaded(channel.id)
                channelViewModel.updateLastCategoryLoaded(playerViewModel.currentCategoryId.value ?: -1L)
            }

            val streamSources = channel.streamSources
            Log.i(TAG, streamSources.toString())
            if (streamSources.isNotEmpty()) {
                loadStreamSource(streamSources.minBy { it.index })
                playerViewModel.updateTriesCountForEachSource(0)
                playerViewModel.updateSourcesTriedCount(0)
            }
            else{
                println("No stream sources found for channel: $channel")
            }
        }
    }

    private fun loadStreamSource(streamSource: StreamSourceItem) {
        if (::jobLoadStreamSource.isInitialized && (jobLoadStreamSource.isActive)) {
            jobLoadStreamSource.cancel()
        }
        jobLoadStreamSource = lifecycleScope.launch {
            if (playerViewModel.isSourceLoading.value == true) timerManager.cancelSourceLoadingTimer()
            if (playerViewModel.isBuffering.value == true) timerManager.cancelBufferingTimer()
            timerManager.cancelCheckPlayingCorrectlyTimer()
            timerManager.cancelSourceLoadingTimer()
            timerManager.cancelBufferingTimer()

            if (playerViewModel.currentStreamSource.value?.id != streamSource.id) {
                playerViewModel.updateIsQualityForced(false)
                playerViewModel.hidePlayer()
            }

            if (player.isLoading || player.isPlaying) {
                player.stop()
            }

            if (playerViewModel.isSourceForced.value == true && playerViewModel.isChannelLoading.value == false){
                timerManager.cancelLoadingIndicatorTimer()
                timerManager.startLoadingIndicatorTimer {
                    if (playerViewModel.isAnimatedLoadingIconVisible.value == false) {
                        Log.i(TAG, "Channel Loading Timeout")
                        if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                        playerViewModel.showAnimatedLoadingIcon()
                    }
                }
            }

            playerViewModel.setIsSourceLoading(true)

            timerManager.startSourceLoadingTimer {
                if (playerViewModel.isSourceLoading.value == true) {
                    Log.i("PlayerFragment", "Source loading timed out")
                    playerViewModel.setIsSourceLoading(false)

                    // Retry logic
                    val currentChannel = playerViewModel.currentChannel.value
                    val currentStreamSource = playerViewModel.currentStreamSource.value
                    if (currentChannel != null && currentStreamSource != null) {
                        lifecycleScope.launch { streamSourceManager.tryNextStreamSource(currentChannel, currentStreamSource) }
                    }
                }
            }
            resetMediaInfo()
            if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
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

    private fun launchUiWithTimeout(
        timeout: Long,
        epgDelay: Long = 0,
        updateEpg: Boolean = true,
        cancelJob: KMutableProperty0<Job?> = ::jobUITimeout,
        extraAfterTimeout: (suspend () -> Unit)? = null,
        showUi: () -> Unit,
        hideUi: () -> Unit
    ) {
        if (cancelJob.get()?.isActive == true) cancelJob.get()?.cancel()
        cancelJob.set(null)

        if (updateEpg) {
            if (::jobEPGRender.isInitialized && jobEPGRender.isActive) jobEPGRender.cancel()
            jobEPGRender = lifecycleScope.launch {
                val currentChannel = playerViewModel.currentChannel.value
                if (currentChannel != null) {
                    if (epgDelay > 0) delay(epgDelay)
                    channelViewModel.updateCurrentProgramForChannel(currentChannel.id)
                }
            }
        }

        cancelJob.set(
            lifecycleScope.launch {
                try {
                    activity?.runOnUiThread { showUi() }
                    delay(timeout)
                    ensureActive()

                    // Custom action before hiding UI (e.g., change channel)
                    extraAfterTimeout?.invoke()

                    activity?.runOnUiThread { hideUi() }
                } catch (_: CancellationException) { }
            }
        )
    }

    private fun showChannelInfoWithTimeout() {
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

    private fun showFullChannelUIWithTimeout(timeout: Long = 4000) {
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

    private fun showChannelNumberWithTimeoutAndChangeChannel() {
        launchUiWithTimeout(
            timeout = 3000L,
            updateEpg = false,
            cancelJob = ::jobUIChangeChannel,
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
                    playerViewModel.hideChannelNumberKeyboard()
                    if (newChannel.id != playerViewModel.currentChannel.value?.id) {
                        playerViewModel.updateCurrentCategoryId(-1L)
                        playerViewModel.updateCategoryName("Favoritos")
                        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        channelViewModel.updateLastCategoryLoaded(-1L)
                        channelViewModel.updateIsLoadingChannel(false)
                        loadChannel(newChannel)
                    } else {
                        playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                        channelViewModel.updateIsLoadingChannel(false)
                        showChannelInfoWithTimeout()
                    }
                } catch (_: CancellationException) {
                    playerViewModel.hideChannelNumberKeyboard()
                    channelViewModel.updateIsLoadingChannel(false)
                } catch (_: ChannelNotFoundException) {
                    playerViewModel.hideChannelNumberKeyboard()
                    playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                    channelViewModel.updateIsLoadingChannel(false)
                } catch (_: NumberFormatException) {
                    playerViewModel.hideChannelNumberKeyboard()
                    playerViewModel.updateCurrentNumberInput(playerViewModel.getCurrentNumberInput().clear())
                    channelViewModel.updateIsLoadingChannel(false)
                }
            },
            hideUi = { /* Nothing else to hide */ }
        )
    }

    private fun showStandardButtons() {
        if (!DeviceUtil.isAndroidTV(requireContext())) {
            playerViewModel.showButtonUp()
            playerViewModel.showButtonDown()
            playerViewModel.showButtonChannelList()
            playerViewModel.showButtonSettings()
            playerViewModel.showButtonPiP()
            playerViewModel.showButtonCategoryList()
        }
    }

    private fun hideStandardButtons() {
        if (!DeviceUtil.isAndroidTV(requireContext())) {
            playerViewModel.hideButtonUp()
            playerViewModel.hideButtonDown()
            playerViewModel.hideButtonChannelList()
            playerViewModel.hideButtonSettings()
            playerViewModel.hideButtonPiP()
            playerViewModel.hideButtonCategoryList()
        }
    }

    private fun showChannelInfo(includeTimeDate: Boolean = false) {
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

    private fun hideChannelInfo() {
        playerViewModel.hideChannelNumber()
        playerViewModel.hideChannelName()
        playerViewModel.hideCategoryName()
        playerViewModel.hideTimeDate()
    }

    private fun showMediaAndBottomInfo(alwaysBottom: Boolean = true) {
        if (playerViewModel.isSourceLoading.value == false) {
            playerViewModel.showMediaInfo()
        }
        if (alwaysBottom || channelViewModel.currentProgram.value != null || channelViewModel.nextProgram.value != null) {
            playerViewModel.showBottomInfo()
        }
    }

    private fun hideMediaAndBottomInfo() {
        playerViewModel.hideMediaInfo()
        playerViewModel.hideBottomInfo()
    }

    @SuppressLint("RestrictedApi")
    fun handleKeyEvent(event: KeyEvent): Boolean {
        return keyHandler.handle(event)
    }

    private fun loadPreviousChannel(){
        var channel: ChannelItem?
        val currentChannelIndex = if (playerViewModel.currentCategoryId.value == -1L) {
            playerViewModel.currentChannel.value?.indexFavourite!!
        } else {
            playerViewModel.currentChannel.value?.indexGroup!!
        }
        if (::jobSwitchChannel.isInitialized && jobSwitchChannel.isActive) jobSwitchChannel.cancel()
        jobSwitchChannel = lifecycleScope.launch {
            channelViewModel.updateIsLoadingChannel(true)
            channel = channelViewModel.getPreviousChannel(playerViewModel.currentCategoryId.value!!, currentChannelIndex - 1)
            channelViewModel.updateIsLoadingChannel(false)
            loadChannel(channel)
        }

    }

    private fun loadNextChannel(){
        println("loadNextChannel")
        var channel: ChannelItem?
        val currentChannelIndex = if (playerViewModel.currentCategoryId.value == -1L) {
            playerViewModel.currentChannel.value?.indexFavourite!!
        } else if (playerViewModel.currentChannel.value != null) {
            playerViewModel.currentChannel.value?.indexGroup!!
        } else {
            0
        }
        if (::jobSwitchChannel.isInitialized && jobSwitchChannel.isActive) jobSwitchChannel.cancel()
        jobSwitchChannel = lifecycleScope.launch {
            channelViewModel.updateIsLoadingChannel(true)
            channel = channelViewModel.getNextChannel(playerViewModel.currentCategoryId.value!!, currentChannelIndex + 1)
            channelViewModel.updateIsLoadingChannel(false)
            loadChannel(channel)
        }
    }

    private fun showLoadingDots() {
        // Show the loading dots
        val dotContainer = binding.loadingDots
        dotContainer.visibility = View.VISIBLE

        // Start the loading animation
        animatorSet = createLoadingAnimation()
        animatorSet.start()
    }

    private fun hideLoadingDots() {
        // Hide the loading dots
        val dotContainer = binding.loadingDots
        dotContainer.visibility = View.GONE

        // Stop and cancel all animations to save resources
        if (this::animatorSet.isInitialized) {
            animatorSet.cancel()
        }

        // Reset dot properties in case they were scaled or faded
        resetDotProperties()
    }

    private fun createLoadingAnimation(): AnimatorSet {
        // Get references to the dots
        val dot1 = binding.dot1
        val dot2 = binding.dot2
        val dot3 = binding.dot3

        // Create a pulsating animation for each dot
        val dot1Animation = createDotAnimation(dot1, 0)
        val dot2Animation = createDotAnimation(dot2, 150)
        val dot3Animation = createDotAnimation(dot3, 300)

        // Play animations together
        return AnimatorSet().apply {
            playTogether(dot1Animation, dot2Animation, dot3Animation)
        }
    }

    private fun createDotAnimation(dot: View, startDelay: Long): AnimatorSet {
        // Scale and fade animation
        val scaleX = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 1.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 1.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.5f, 1f)

        // Set duration and delay
        scaleX.duration = 500
        scaleY.duration = 500
        alpha.duration = 500
        scaleX.startDelay = startDelay
        scaleY.startDelay = startDelay
        alpha.startDelay = startDelay

        // Repeat infinitely
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        alpha.repeatCount = ObjectAnimator.INFINITE

        // Play animations together
        return AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
        }
    }

    private fun resetDotProperties() {
        // Reset each dot to its original properties
        val dots = listOf(activity?.findViewById<View>(R.id.dot1), activity?.findViewById(R.id.dot2), activity?.findViewById(R.id.dot3))
        for (dot in dots) {
            dot?.scaleX = 1f
            dot?.scaleY = 1f
            dot?.alpha = 1f
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart")
        channelViewModel.startEpgAutoRefresh()
        player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
        if (isInEPGPictureInPictureMode) {
            player.playWhenReady = true
            playerViewModel.hideButtonPiP()
            playerViewModel.hideButtonSettings()
            playerViewModel.hideButtonUp()
            playerViewModel.hideButtonDown()
            playerViewModel.hideButtonChannelList()
            playerViewModel.hideButtonCategoryList()
            playerViewModel.hideChannelList()
            playerViewModel.hideSettingsMenu()
            playerViewModel.hideTrackMenu()
            playerViewModel.hideMediaInfo()
            playerViewModel.hideChannelName()
            playerViewModel.hideChannelNumber()
            playerViewModel.hideChannelNumberKeyboard()
            playerViewModel.hideTimeDate()
            playerViewModel.hideBottomInfo()
        }
        else{
            player.playWhenReady = false
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        if (playerViewModel.currentStreamSource.value != null && !activity?.isInPictureInPictureMode!!) {
            loadStreamSource(playerViewModel.currentStreamSource.value!!)
        }
        player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop")
        //ProxySelector.setDefault(originalProxySelector)
        channelViewModel.stopEpgAutoRefresh()
        if (!isInEPGPictureInPictureMode){
            player.playWhenReady = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        //ProxySelector.setDefault(originalProxySelector)
        player.release()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPiPModeIfSupported() {
        val aspectRatio = Rational(16, 9)
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        requireActivity().enterPictureInPictureMode(pipParams)
    }

    /*override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.i(TAG, "onUserLeaveHint")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isAndroidTV(this)) {
                super.onUserLeaveHint()
                enterPiPMode()
            }
        }
    }*/

    companion object {
        val TAG: String = PlayerFragment::class.java.name
    }
}

fun View.bindVisibility(
    lifecycleOwner: LifecycleOwner,
    liveData: LiveData<Boolean>,
    invisibleInsteadOfGone: Boolean = false
) {
    liveData.observe(lifecycleOwner) { isVisible ->
        visibility = if (isVisible) {
            View.VISIBLE
        } else {
            if (invisibleInsteadOfGone) View.INVISIBLE else View.GONE
        }
    }
}

fun orLiveData(a: LiveData<Boolean>, b: LiveData<Boolean>): LiveData<Boolean> {
    return MediatorLiveData<Boolean>().apply {
        val observer = Observer<Any> {
            value = a.value == true || b.value == true
        }
        addSource(a, observer)
        addSource(b, observer)
    }
}
