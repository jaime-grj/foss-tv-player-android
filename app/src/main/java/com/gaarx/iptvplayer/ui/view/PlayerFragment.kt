package com.gaarx.iptvplayer.ui.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.core.MediaUtils
import com.gaarx.iptvplayer.core.ytlivedashmanifestparser.LiveDashManifestParser
import com.gaarx.iptvplayer.data.services.ApiService
import com.gaarx.iptvplayer.databinding.FragmentPlayerBinding
import com.gaarx.iptvplayer.domain.model.AudioTrack
import com.gaarx.iptvplayer.domain.model.CategoryItem
import com.gaarx.iptvplayer.domain.model.ChannelItem
import com.gaarx.iptvplayer.domain.model.ChannelSettings
import com.gaarx.iptvplayer.domain.model.DrmTypeItem
import com.gaarx.iptvplayer.domain.model.MediaInfo
import com.gaarx.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarx.iptvplayer.domain.model.StreamSourceItem
import com.gaarx.iptvplayer.domain.model.StreamSourceTypeItem
import com.gaarx.iptvplayer.domain.model.SubtitlesTrack
import com.gaarx.iptvplayer.domain.model.VideoTrack
import com.gaarx.iptvplayer.exceptions.ChannelNotFoundException
import com.gaarx.iptvplayer.ui.adapters.AudioTracksAdapter
import com.gaarx.iptvplayer.ui.adapters.CategoryListAdapter
import com.gaarx.iptvplayer.ui.adapters.ChannelListAdapter
import com.gaarx.iptvplayer.ui.adapters.ChannelSettingsAdapter
import com.gaarx.iptvplayer.ui.adapters.ChannelSourcesAdapter
import com.gaarx.iptvplayer.ui.adapters.NumberListAdapter
import com.gaarx.iptvplayer.ui.adapters.SubtitlesTracksAdapter
import com.gaarx.iptvplayer.ui.adapters.VideoTracksAdapter
import com.gaarx.iptvplayer.ui.viewmodel.ChannelViewModel
import com.gaarx.iptvplayer.ui.viewmodel.PlayerViewModel
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.chromium.net.CronetEngine
import java.net.ProxySelector
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.flow.collectLatest


@UnstableApi
@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var animatorSet: AnimatorSet

    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector

    private var mediaInfo: MediaInfo = MediaInfo()

    private var currentNumberInput = StringBuilder()

    private lateinit var jobUITimeout: Job
    private lateinit var jobLoadStreamSource: Job
    private lateinit var jobUIChangeChannel: Job
    private lateinit var jobEPGRender : Job
    private lateinit var jobFastChangeChannel : Job
    private lateinit var jobSwitchChannel : Job

    private val handler = Handler(Looper.getMainLooper())

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

    data class CachedUrl(val url: String, val timestamp: Long)
    private val urlCache: MutableMap<StreamSourceItem, CachedUrl> = ConcurrentHashMap()
    private val cacheExpirationTime = TimeUnit.MINUTES.toMillis(TIME_CACHED_URL_MINUTES)

    private var originalProxySelector: ProxySelector? = null

    private var isActivityPaused: Boolean = false
    private var isActivityStopped: Boolean = false

    private var currentAudioTrack: AudioTrack? = null
    private var currentVideoTrack: VideoTrack? = null
    private var currentSubtitlesTrack: SubtitlesTrack? = null

    private var isLongPressDown: Boolean = false
    private var isLongPressUp: Boolean = false
    private var channelIdFastSwitch: Int = 0

    private var hasReceivedChannelId = false

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val resultData = data?.getLongExtra("channelId", 0L)
            if (resultData != null) {
                lifecycleScope.launch {
                    channelViewModel.updateIsLoadingChannel(true)
                    val newChannel = channelViewModel.getChannelById(resultData)
                    if (newChannel != null) {
                        channelViewModel.updateCurrentCategoryId(-1L)
                        channelViewModel.updateLastCategoryLoaded(-1L)
                        playerViewModel.updateCategoryName("Favoritos")
                        channelViewModel.updateIsLoadingChannel(false)
                        loadChannel(newChannel)
                    }
                }
            }
        }
    }

    private val tryNextStreamSourceRunnable = Runnable {
        Log.i(TAG, "Try Next Stream Source")
        val currentChannel = channelViewModel.currentChannel.value ?: return@Runnable
        val currentStreamSource = playerViewModel.currentStreamSource.value ?: return@Runnable
        tryNextStreamSource(currentChannel, currentStreamSource)
    }

    private val sourceLoadingRunnable = Runnable {
        if (playerViewModel.isSourceLoading.value == true) {
            Log.i(TAG, "Loading Timeout")
            val currentChannel = channelViewModel.currentChannel.value ?: return@Runnable
            val currentStreamSource = playerViewModel.currentStreamSource.value ?: return@Runnable
            tryNextStreamSource(currentChannel, currentStreamSource)
        }
    }

    private val bufferingRunnable = Runnable {
        if (playerViewModel.isBuffering.value == true) {
            Log.i(TAG, "Buffering Timeout")
            val currentChannel = channelViewModel.currentChannel.value ?: return@Runnable
            val currentStreamSource = playerViewModel.currentStreamSource.value ?: return@Runnable
            playerViewModel.hidePlayer()
            tryNextStreamSource(currentChannel, currentStreamSource)
        }
    }

    private val checkPlayingCorrectlyRunnable = Runnable {
        playerViewModel.updateSourcesTriedCount(1)
        playerViewModel.updateTriesCountForEachSource(1)
    }

    private val channelLoadingRunnable = Runnable {
        if (playerViewModel.isAnimatedLoadingIconVisible.value == false) {
            Log.i(TAG, "Channel Loading Timeout")
            if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
            playerViewModel.showAnimatedLoadingIcon()
        }
    }

    private val hidePlayerRunnable = Runnable {
        playerViewModel.hidePlayer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener("channelRequestKey", this) { _, bundle ->
            val channelId = bundle.getLong("channelId", 0L)
            Log.d("PlayerFragment", "Received channelId: $channelId")
            playerViewModel.setIncomingChannelId(channelId)
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
        originalProxySelector = ProxySelector.getDefault()
        initPlayer()


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
                        channelViewModel.updateCurrentCategoryId(-1L)
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
                        channelViewModel.updateIsImportingData(true)
                        channelViewModel.importJSONData()
                        channelViewModel.updateIsImportingData(false)
                        channelViewModel.updateEPG()
                        channelViewModel.updateCurrentCategoryId(-1L)
                        playerViewModel.updateCategoryName("")
                        try {
                            loadChannel(channelViewModel.getChannel(-1L, 1))
                        } catch (e: Exception) {
                            Log.e("PlayerActivity", "Error: ${e.message}")
                        }
                    } else if (lastChannelId != 0L && lastCategoryId != 0L) {
                        val channel = channelViewModel.getChannelById(lastChannelId)
                        println("lastcategoryid: $lastCategoryId")
                        if (channel != null) {
                            channelViewModel.updateCurrentCategoryId(lastCategoryId)
                            val category = channelViewModel.getCategoryById(lastCategoryId)
                            Log.i("PlayerActivity", "category: $category")
                            if (category != null) {
                                playerViewModel.updateCategoryName(category.name)
                            }
                            loadChannel(channel)
                        } else {
                            channelViewModel.updateCurrentCategoryId(-1L)
                            playerViewModel.updateCategoryName("")
                            loadChannel(channelViewModel.getPreviousChannel(-1L, 1))
                        }
                    } else {
                        channelViewModel.updateCurrentCategoryId(-1L)
                        playerViewModel.updateCategoryName("")
                        loadChannel(channelViewModel.getPreviousChannel(-1L, 1))
                    }
                    channelViewModel.updateIsLoadingChannel(false)
                    initCategoryList()
                }
            }
        }
    }

    private fun isAndroidTV(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        playerViewModel.onCreate()
        lifecycleScope.launch {
            channelViewModel.downloadEPG()
        }
        playerViewModel.updateCurrentItemSelectedFromChannelList(0)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.setDecorFitsSystemWindows(false)
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

        rvChannelList = binding.channelList
        rvChannelSettings = binding.rvChannelSettings
        rvAudioTracks = binding.rvChannelTrackSettings
        rvSubtitlesTracks = binding.rvChannelTrackSettings
        rvChannelSources = binding.rvChannelTrackSettings
        rvVideoTracks = binding.rvChannelTrackSettings
        rvCategoryList = binding.rvCategoryList
        rvNumberList = binding.rvNumberList

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

        playerViewModel.isChannelListVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.channelList.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isSettingsMenuVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.rvChannelSettings.visibility = if (isVisible){
                View.VISIBLE

            } else{
                View.GONE
            }
        }

        playerViewModel.isTrackMenuVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.rvChannelTrackSettings.visibility = if (isVisible) View.VISIBLE else View.GONE
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

        playerViewModel.isMediaInfoVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.channelMediaInfo.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isChannelNameVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.channelName.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isChannelNumberVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.channelNumber.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        }

        playerViewModel.isTimeDateVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.timeDate.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isErrorMessageVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.message.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isBottomErrorMessageVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.bottomErrorMessage.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.bottomErrorMessage.observe(viewLifecycleOwner) { bottomErrorMessage ->
            binding.bottomErrorMessage.text = bottomErrorMessage
        }

        playerViewModel.isAnimatedLoadingIconVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) showLoadingDots() else hideLoadingDots()
        }

        playerViewModel.isPlayerVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.playerView.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonUpVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.buttonUp.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonDownVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.buttonDown.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonSettingsVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.buttonSettings.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonChannelListVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.buttonChannelList.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonPiPVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.buttonPiP.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonCategoryListVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.buttonCategory.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isChannelNumberKeyboardVisible.observe(viewLifecycleOwner) { isVisible ->
            binding.channelNumberKeyboard.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

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

        playerViewModel.isBottomInfoVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                binding.channelBottomInfo.visibility = View.VISIBLE
            }
            else{
                binding.channelBottomInfo.visibility = View.GONE
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

        playerViewModel.isCategoryNameVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                binding.categoryName.visibility = View.VISIBLE
            }
            else{
                binding.categoryName.visibility = View.GONE
            }
        }

        playerViewModel.isNumberListMenuVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                binding.rvNumberList.visibility = View.VISIBLE
            }
            else{
                binding.rvNumberList.visibility = View.GONE
            }
        }

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
                if (!isAndroidTV(requireContext())) {
                    playerViewModel.hideButtonUp()
                    playerViewModel.hideButtonDown()
                    playerViewModel.hideButtonChannelList()
                    playerViewModel.hideButtonSettings()
                    playerViewModel.hideButtonPiP()
                    playerViewModel.hideButtonCategoryList()
                }
                if (!isLongPressDown && !isLongPressUp) {
                    playerViewModel.hideChannelNumber()
                }
                playerViewModel.hideChannelName()
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
            ChannelSettings("Actualizar EPG"),
            ChannelSettings(getString(R.string.epg)),
            ChannelSettings("Actualizar lista canales"),
            ChannelSettings("URL de configuración")
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
            loadAudioTrack(selectedAudioTrack)
        }
    }

    private fun initSubtitlesTracksMenu() {
        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(listOf()) { selectedSubtitlesTrack ->
            loadSubtitlesTrack(selectedSubtitlesTrack)
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
            }
            ChannelSettings.UPDATE_CHANNEL_LIST -> {
                lifecycleScope.launch {
                    channelViewModel.importJSONData()
                    initCategoryList()
                    channelViewModel.updateCurrentCategoryId(-1L)
                    playerViewModel.updateCategoryName("Favoritos")
                    try{
                        val firstChannel = channelViewModel.getChannel(-1L, 1)
                        loadChannel(firstChannel)
                    }
                    catch (e: Exception){
                        Toast.makeText(requireContext(), "Error al cargar la lista de canales", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadAudioTracksMenu(){
        val audioTrackList = loadAudioTracks()
        rvAudioTracks.adapter = AudioTracksAdapter(audioTrackList) { selectedAudioTrack ->
            loadAudioTrack(selectedAudioTrack)
        }
        playerViewModel.showTrackMenu()
        rvAudioTracks.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.AUDIO_TRACKS)
    }

    private fun loadSubtitlesTracksMenu() {
        val subtitlesTrackList = loadSubtitlesTracks()
        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(subtitlesTrackList) { selectedSubtitlesTrack ->
            loadSubtitlesTrack(selectedSubtitlesTrack)
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
            loadVideoTrack(selectedVideoTrack)
        }
        playerViewModel.showTrackMenu()
        rvVideoTracks.requestFocus()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.VIDEO_TRACKS)

    }

    private fun loadSourcesMenu() {
        val sourcesList = mutableListOf(
            StreamSourceItem(id = -1,
                name = getString(R.string.auto),
                url = "",
                apiCalls = listOf(),
                headers = listOf(),
                index = -1,
                streamSourceType = StreamSourceTypeItem.IPTV,
                isSelected = playerViewModel.isSourceForced.value == false,
                drmType = DrmTypeItem.NONE
            )
        )

        sourcesList += channelViewModel.currentChannel.value?.streamSources!!.sortedBy { it.index }
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

    private fun loadAudioTracks() : List<AudioTrack>{
        val audioTrackList: MutableList<AudioTrack> = mutableListOf()
        val currentTracks = player.currentTracks
        var globalAudioTrackIndex = 0

        for (i in 0 until currentTracks.groups.size) {
            val trackGroup = currentTracks.groups[i]
            if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                for (j in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(j)
                    println(trackFormat)
                    audioTrackList += listOf(AudioTrack(trackFormat.id.orEmpty(), trackFormat.language.orEmpty(), trackFormat.codecs ?: trackFormat.sampleMimeType.orEmpty(), trackFormat.bitrate, trackFormat.channelCount, trackGroup.isTrackSelected(j)))
                    globalAudioTrackIndex++
                }
            }
        }
        return audioTrackList
    }

    private fun loadSubtitlesTracks() : List<SubtitlesTrack>{
        val subtitlesTrackList: MutableList<SubtitlesTrack> = mutableListOf()
        val currentTracks = player.currentTracks
        var globalSubtitleTrackIndex = 0
        subtitlesTrackList += listOf(SubtitlesTrack("-1", getString(R.string.off), "", true))

        for (i in 0 until currentTracks.groups.size) {
            val trackGroup = currentTracks.groups[i]
            if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                for (j in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(j)
                    if (trackGroup.isTrackSelected(j)) {
                        subtitlesTrackList.first().isSelected = false
                    }
                    subtitlesTrackList += listOf(SubtitlesTrack(trackFormat.id.orEmpty(), trackFormat.language.orEmpty(), trackFormat.codecs ?: trackFormat.sampleMimeType.orEmpty(), trackGroup.isTrackSelected(j)))
                    globalSubtitleTrackIndex++
                }
            }
        }
        return subtitlesTrackList
    }

    private fun loadVideoTracks() : List<VideoTrack>{
        val videoTrackList: MutableList<VideoTrack> = mutableListOf()
        val currentTracks = player.currentTracks
        var globalVideoTrackIndex = 0
        videoTrackList += listOf(VideoTrack("-1", getString(R.string.auto), -1, -1, 0, "", playerViewModel.isQualityForced.value == false))

        for (i in 0 until currentTracks.groups.size) {
            val trackGroup = currentTracks.groups[i]
            if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                for (j in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(j)
                    println(trackFormat)
                    videoTrackList += if (playerViewModel.isQualityForced.value == false) {
                        listOf(VideoTrack(trackFormat.id.orEmpty(), trackFormat.codecs.orEmpty(), trackFormat.width, trackFormat.height, trackFormat.bitrate / 1000, trackFormat.codecs ?: trackFormat.sampleMimeType.orEmpty(), false))
                    } else{
                        listOf(VideoTrack(trackFormat.id.orEmpty(), trackFormat.codecs.orEmpty(), trackFormat.width, trackFormat.height, trackFormat.bitrate / 1000, trackFormat.codecs ?: trackFormat.sampleMimeType.orEmpty(), trackGroup.isTrackSelected(j)))
                    }
                    globalVideoTrackIndex++
                }
            }
        }
        return videoTrackList
    }

    private fun initCategoryList() {
        lifecycleScope.launch {
            val categories = listOf(CategoryItem(-1L, "Favoritos", null, false, listOf())) + channelViewModel.getCategories()
            Log.i("PlayerActivity", "initCategoryList: ${categories.size}")
            rvCategoryList.adapter = CategoryListAdapter(categories) { selectedCategory ->
                channelViewModel.updateIsLoadingChannel(true)
                channelViewModel.updateCurrentCategoryId(selectedCategory.id)
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
        val sortedChannels = channelViewModel.getSmChannelsByCategory(channelViewModel.currentCategoryId.value!!)
        for (i in sortedChannels.indices) {
            if (sortedChannels[i].id == channelViewModel.currentChannel.value?.id) {
                playerViewModel.updateCurrentItemSelectedFromChannelList(i)
                break
            }
        }
        rvChannelList.adapter = ChannelListAdapter(channelViewModel.currentCategoryId.value!!, sortedChannels) { selectedChannel ->
            loadChannel(selectedChannel)
        }
    }

    private fun initNumberList(){
        rvNumberList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        rvNumberList.adapter = NumberListAdapter(numbers) { selectedNumber ->
            if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) {
                jobUIChangeChannel.cancel()
            }
            if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
            if (playerViewModel.isChannelNameVisible.value == true) playerViewModel.hideChannelName()
            if (playerViewModel.isChannelNumberVisible.value == true) playerViewModel.hideChannelNumber()
            if (playerViewModel.isCategoryNameVisible.value == true) playerViewModel.hideCategoryName()
            if (currentNumberInput.length >= MAX_DIGITS) {
                currentNumberInput.clear()
            }

            val number = (selectedNumber).toString()

            currentNumberInput.append(number)

            binding.channelNumberKeyboard.text = currentNumberInput.toString()

            showChannelNumberWithTimeoutAndChangeChannel(3000)
        }

    }

    private fun initPlayer(){
        binding.playerView.useController = false

        trackSelector = DefaultTrackSelector(requireContext())
        val parameters = trackSelector
            .buildUponParameters()
            //.setForceHighestSupportedBitrate(true) // Choose the highest quality
            //.setAllowVideoMixedMimeTypeAdaptiveness(false) // Disable adaptive switching between codecs
            //.setMaxVideoSize(1920, 1080)
            //.setMaxVideoBitrate(80000000)
            //.setExceedRendererCapabilitiesIfNecessary(true)
            .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        trackSelector.parameters = parameters

        val renderersFactory = DefaultRenderersFactory( requireContext())
        renderersFactory.setExtensionRendererMode(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        )

        /*val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1250,  // Min buffer before starting playback
                25000,  // Max buffer size
                1250,   // Min buffer for playback start
                1250    // Min buffer for playback resume after a pause
            ).build()*/

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS, // Minimum buffer before playback
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS, // Maximum buffer during playback
                2000, // Buffer before playback starts
                1500 // Buffer after rebuffering
            )
            .build()

        val bandwidthMeter: DefaultBandwidthMeter = DefaultBandwidthMeter.Builder(requireContext())
            .setInitialBitrateEstimate(50_000_000)
            .build()

        player = ExoPlayer.Builder(requireContext())
            .setTrackSelector(trackSelector).setRenderersFactory(
                renderersFactory
                    .setEnableDecoderFallback(true)
                    .setMediaCodecSelector(MediaCodecSelector.DEFAULT))
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .setDetachSurfaceTimeoutMs(4000)
            .build()

        setSubtitleTheme()

        binding.playerView.player = player

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                startHidePlayerTimer()
                Log.e("PlayerError", "Player error: ${error.message}")
                if (error is ExoPlaybackException) {
                    when (error.type) {
                        ExoPlaybackException.TYPE_SOURCE -> {
                            Log.e("PlayerError", "Source error: ${error.sourceException.message}")
                            playerViewModel.updateBottomErrorMessage("${error.sourceException.message}")
                            playerViewModel.showBottomErrorMessage()
                        }
                        ExoPlaybackException.TYPE_RENDERER -> {
                            Log.e("PlayerError", "Renderer error: ${error.rendererException.message}")
                            playerViewModel.updateBottomErrorMessage("${error.rendererException.message}")
                            playerViewModel.showBottomErrorMessage()
                        }
                        ExoPlaybackException.TYPE_UNEXPECTED -> {
                            Log.e("PlayerError", "Unexpected error: ${error.unexpectedException.message}")
                            playerViewModel.updateBottomErrorMessage("${error.unexpectedException.message}")
                            playerViewModel.showBottomErrorMessage()
                        }
                        // Handle other types of errors as needed
                        ExoPlaybackException.TYPE_REMOTE -> {
                            Log.e("PlayerError", "Unexpected error: ${error.unexpectedException.message}")
                        }
                        else -> {
                            Log.e("PlayerError", "Unknown error: ${error.message}")
                        }
                    }
                }
                if (!isActivityStopped) handler.postDelayed(tryNextStreamSourceRunnable, RETRY_DELAY_MS)
            }

            override fun onRenderedFirstFrame() {
                if (playerViewModel.isErrorMessageVisible.value == true) playerViewModel.hideErrorMessage()
                playerViewModel.hideBottomErrorMessage()
                if (playerViewModel.isAnimatedLoadingIconVisible.value == true) playerViewModel.hideAnimatedLoadingIcon()
                cancelLoadingIndicatorTimer()
                cancelHidePlayerTimer()
                if (currentSubtitlesTrack != null) loadSubtitlesTrack(currentSubtitlesTrack!!)
                println(currentAudioTrack)
                if (currentAudioTrack != null) loadAudioTrack(currentAudioTrack!!)
                if (currentVideoTrack != null){
                    loadVideoTrack(currentVideoTrack!!)
                }
                if (playerViewModel.currentStreamSource.value?.drmType != DrmTypeItem.NONE) {
                    mediaInfo.hasDRM = true
                    playerViewModel.updateMediaInfo(mediaInfo)
                }
                if (playerViewModel.currentStreamSource.value?.forceUseBestVideoResolution == true && playerViewModel.isQualityForced.value != true){ // Select best quality by default (some streams fail)
                    Log.i(TAG, "forceUseBestVideoResolution")
                    val tracks = player.currentTracks
                    val highestResolution = getHighestResolution()
                    for (trackGroup in tracks.groups) {
                        if (trackGroup.type == C.TRACK_TYPE_VIDEO){
                            for (i in 0 until trackGroup.length) {
                                val trackFormat = trackGroup.getTrackFormat(i)
                                if (highestResolution == trackFormat.height) {
                                    player.trackSelectionParameters =
                                        player.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(
                                                TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                            )
                                            .build()
                                    currentVideoTrack = VideoTrack(trackFormat.id.orEmpty(), trackFormat.codecs.orEmpty(), trackFormat.width, trackFormat.height, trackFormat.bitrate / 1000, trackFormat.codecs ?: trackFormat.sampleMimeType.orEmpty(), true)
                                    playerViewModel.updateIsQualityForced(true)
                                }
                            }
                        }
                    }
                }

            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (playerViewModel.isSourceLoading.value == true) cancelSourceLoadingTimer()
                    if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
                    cancelTryNextStreamSourceTimer()
                    playerViewModel.hideErrorMessage()
                    playerViewModel.hideBottomErrorMessage()
                    playerViewModel.showPlayer()
                    /*val mediaItem = player.currentMediaItem
                    mediaItem?.let { item ->
                        val drmConfiguration = item.localConfiguration?.drmConfiguration
                        if (drmConfiguration != null) {
                            Log.d("DRM_CHECK", "Stream has DRM with config: $drmConfiguration")
                            mediaInfo.hasDRM = true
                            playerViewModel.updateMediaInfo(mediaInfo)
                        } else {
                            Log.d("DRM_CHECK", "Stream does not have DRM.")
                            mediaInfo.hasDRM = false
                            playerViewModel.updateMediaInfo(mediaInfo)
                        }
                    }*/
                }
                else if (playbackState == Player.STATE_ENDED){
                    Log.i(TAG, "player ended")
                    if (playerViewModel.isSourceLoading.value == true) cancelSourceLoadingTimer()
                    if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
                    cancelCheckPlayingCorrectlyTimer()
                    if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                    startHidePlayerTimer()
                    if (playerViewModel.isSourceLoading.value == false && playerViewModel.isBuffering.value == false) {
                        if (!isActivityStopped) handler.postDelayed(tryNextStreamSourceRunnable, RETRY_DELAY_MS)
                    }
                }
                else if (playbackState == Player.STATE_BUFFERING){
                    Log.i(TAG, "buffering")
                    if (playerViewModel.isPlayerVisible.value != true){
                        playerViewModel.showPlayer()
                    }
                    cancelCheckPlayingCorrectlyTimer()
                    if (playerViewModel.isSourceLoading.value == false) startBufferingTimer()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (playerViewModel.isSourceLoading.value == true) cancelSourceLoadingTimer()
                    if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
                    if (playerViewModel.isChannelLoading.value == true) cancelLoadingIndicatorTimer()
                    cancelTryNextStreamSourceTimer()
                    cancelHidePlayerTimer()

                    playerViewModel.hideAnimatedLoadingIcon()
                    playerViewModel.hideErrorMessage()
                    playerViewModel.hideBottomErrorMessage()
                    //playerViewModel.showPlayer()


                    when (playerViewModel.currentLoadedMenuSetting.value) {
                        ChannelSettings.AUDIO_TRACKS -> {
                            val audioTrackList = loadAudioTracks()

                            rvAudioTracks.adapter = AudioTracksAdapter(audioTrackList) { selectedAudioTrack ->
                                loadAudioTrack(selectedAudioTrack)
                            }
                            rvAudioTracks.requestFocus()
                        }
                        ChannelSettings.SUBTITLES_TRACKS -> {
                            val subtitlesTrackList = loadSubtitlesTracks()
                            rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(subtitlesTrackList) { selectedSubtitlesTrack ->
                                loadSubtitlesTrack(selectedSubtitlesTrack)
                            }
                            rvSubtitlesTracks.requestFocus()
                        }
                        ChannelSettings.VIDEO_TRACKS -> {
                            val videoTrackList = loadVideoTracks()
                            val isQualityForced = playerViewModel.isQualityForced.value!!
                            rvVideoTracks.adapter = VideoTracksAdapter(isQualityForced, videoTrackList) { selectedVideoTrack ->
                                loadVideoTrack(selectedVideoTrack)
                            }
                            rvVideoTracks.requestFocus()
                        }
                    }

                    // Dynamic FPS calculation (doesn't work very well)
                    /*val videoCounters = player.videoDecoderCounters
                    val framesRendered1 = videoCounters?.renderedOutputBufferCount
                    val startTimeMs = SystemClock.uptimeMillis()
                    if (::jobCalculateFrameRate.isInitialized && jobCalculateFrameRate.isActive) {
                        jobCalculateFrameRate.cancel()
                    }

                    jobCalculateFrameRate = CoroutineScope(Dispatchers.IO).launch{
                        delay(5000)
                        val elapsedTimeMs = SystemClock.uptimeMillis() - startTimeMs
                        val framesRendered2 = videoCounters?.renderedOutputBufferCount
                        val frameRate = (framesRendered2?.minus(framesRendered1!!))?.div((elapsedTimeMs / 1000f))
                        if (mediaInfo.videoFrameRate == null) {
                            runOnUiThread{
                                mediaInfo.videoFrameRate = (frameRate?.roundToInt().toString() + " FPS")
                                val frameRateSwitch: Float
                                if (frameRate != null){
                                    if (areRatesEqual(frameRate, 25.0f)){
                                        frameRateSwitch = 50.0f
                                    }
                                    else if (areRatesEqual(frameRate, 30.0f)){
                                        frameRateSwitch = 60.0f
                                    }
                                    else if (frameRate > 0.0f){
                                        frameRateSwitch = frameRate
                                    }
                                    else{
                                        frameRateSwitch = 50.0f
                                    }
                                    switchRefreshRate(frameRateSwitch)
                                }
                                playerViewModel.updateMediaInfo(mediaInfo)
                            }
                        }

                    }*/

                    if (binding.channelName.isVisible && playerViewModel.isSourceLoading.value == false) {
                        playerViewModel.showMediaInfo()
                    }
                    startCheckPlayingCorrectlyTimer()
                }
                binding.playerView.keepScreenOn = isPlaying
            }

            override fun onTracksChanged(tracks: Tracks) {
                var audioTracksCount = 0
                for (i in 0 until tracks.groups.size) {
                    val trackGroup = tracks.groups[i]
                    if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                        audioTracksCount += 1
                        for (j in 0 until trackGroup.length) {
                            val trackFormat = trackGroup.getTrackFormat(j)
                            Log.i(TAG, trackFormat.toString())
                            if (trackGroup.isTrackSelected(j)){
                                val audioCodec: String?
                                audioCodec = if (trackFormat.codecs != null) {
                                    trackFormat.codecs
                                } else {
                                    trackFormat.sampleMimeType
                                }
                                MediaUtils.getUserFriendlyCodec(audioCodec)?.let { mediaInfo.audioCodec = it }
                                if (trackFormat.bitrate > 0){
                                    mediaInfo.audioBitrate = (trackFormat.bitrate / 1000).toString() + " kbps"
                                } else{
                                    mediaInfo.audioBitrate = null
                                }
                                when (trackFormat.channelCount) {
                                    1 -> {
                                        mediaInfo.audioChannels = "Mono"
                                    }
                                    2 -> {
                                        mediaInfo.audioChannels = "Stereo"
                                    }
                                    6 -> {
                                        mediaInfo.audioChannels = "5.1"
                                    }
                                    8 -> {
                                        mediaInfo.audioChannels = "7.1"
                                    }
                                }
                                /*if (trackFormat.sampleRate > 0){
                                    mediaInfo.audioSamplingRate = DecimalFormat("0.#").format(trackFormat.sampleRate.toFloat() / 1000).toString() + " kHz"
                                }
                                else {
                                    mediaInfo.audioSamplingRate = null
                                }*/
                            }
                        }
                        mediaInfo.hasMultipleAudios = audioTracksCount > 1
                        /* Not the best way to get audiodescription status */
                        /*mediaInfo.hasAudioDescription = "ads" in audioLanguages || "qad" in audioLanguages*/
                    }
                    if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                        val subtitlesLanguages = mutableListOf<String?>()
                        for (j in 0 until trackGroup.length) {
                            val trackFormat = trackGroup.getTrackFormat(j)
                            Log.i(TAG, trackFormat.toString())
                            val subtitlesLanguage = trackFormat.language
                            if(subtitlesLanguage !in subtitlesLanguages){
                                subtitlesLanguages += listOf(subtitlesLanguage)
                                Log.i(TAG, subtitlesLanguages.toString())
                            }
                        }
                        Log.i(TAG, subtitlesLanguages.toString())
                        mediaInfo.hasSubtitles = subtitlesLanguages.filterNotNull().isNotEmpty()
                    }
                    val currentProgram = channelViewModel.currentProgram.value
                    val nextProgram = channelViewModel.nextProgram.value
                    if (currentProgram != null || nextProgram != null) {
                        mediaInfo.hasEPG = true
                        playerViewModel.updateMediaInfo(mediaInfo)
                    }
                    else{
                        mediaInfo.hasEPG = false
                        playerViewModel.updateMediaInfo(mediaInfo)
                    }
                    playerViewModel.updateMediaInfo(mediaInfo)
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val aspectRatio = if (videoSize.height > 0) {
                    MediaUtils.getHumanReadableAspectRatio(videoSize.width.toFloat() / videoSize.height * videoSize.pixelWidthHeightRatio)
                } else {
                    null
                }
                mediaInfo.videoAspectRatio = aspectRatio
                if (videoSize.width > 0 && videoSize.height > 0) {
                    mediaInfo.videoResolution = "${videoSize.width}x${videoSize.height}"
                    mediaInfo.videoQuality = MediaUtils.calculateVideoQuality(videoSize.width, videoSize.height)
                }
                else{
                    mediaInfo.videoResolution = null
                    mediaInfo.videoQuality = null
                }
                playerViewModel.updateMediaInfo(mediaInfo)
            }
        })

        player.addAnalyticsListener(object: AnalyticsListener {
            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                val audioCodec: String?
                audioCodec = if (format.codecs != null) {
                    format.codecs
                } else {
                    format.sampleMimeType
                }
                MediaUtils.getUserFriendlyCodec(audioCodec)?.let { mediaInfo.audioCodec = it }
                if (format.bitrate > 0){
                    mediaInfo.audioBitrate =(format.bitrate / 1000).toString() + " kbps"
                } else{
                    mediaInfo.audioBitrate = null
                }
                when (format.channelCount) {
                    1 -> {
                        mediaInfo.audioChannels = "Mono"
                    }
                    2 -> {
                        mediaInfo.audioChannels = "Stereo"
                    }
                    6 -> {
                        mediaInfo.audioChannels = "5.1"
                    }
                    8 -> {
                        mediaInfo.audioChannels = "7.1"
                    }
                }
                mediaInfo.audioSamplingRate = DecimalFormat("0.#").format(format.sampleRate.toFloat() / 1000).toString() + " kHz"
                playerViewModel.updateMediaInfo(mediaInfo)
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                val videoCodec: String?
                videoCodec = if (format.codecs != null) {
                    format.codecs
                } else {
                    format.sampleMimeType
                }
                MediaUtils.getUserFriendlyCodec(videoCodec)?.let { mediaInfo.videoCodec = it }
                if (format.frameRate.toInt() != Format.NO_VALUE){
                    if (format.frameRate % 1.0 == 0.0) {
                        mediaInfo.videoFrameRate = format.frameRate.toInt().toString() + " FPS"
                    } else {
                        mediaInfo.videoFrameRate = String.format(Locale.getDefault(), "%.2f", format.frameRate) + " FPS"
                    }

                } else mediaInfo.videoFrameRate = null
                val frameRateSwitch: Float = if (MediaUtils.areRatesEqual(format.frameRate, 25.0f)){
                    50.0f
                } else if (MediaUtils.areRatesEqual(format.frameRate, 30.0f)){
                    60.0f
                } else if (format.frameRate > 0.0f){
                    format.frameRate
                } else if (playerViewModel.currentStreamSource.value?.refreshRate != null){
                    playerViewModel.currentStreamSource.value?.refreshRate!!.toFloat()
                }
                else{
                    DEFAULT_REFRESH_RATE
                }
                switchRefreshRate(frameRateSwitch)
                mediaInfo.videoAspectRatio = MediaUtils.getHumanReadableAspectRatio(format.width.toFloat() / format.height * format.pixelWidthHeightRatio)
                if (format.bitrate > 0){
                    mediaInfo.videoBitrate = DecimalFormat("0.##").format((format.bitrate.toFloat() / 1_000_000.0)) + " Mbps"
                }
                else mediaInfo.videoBitrate = null
                playerViewModel.updateMediaInfo(mediaInfo)
            }

            override fun onDrmSessionAcquired(eventTime: AnalyticsListener.EventTime, state: Int) {
                Log.d("DRM", "DRM session acquired for content.")
            }

            override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime) {
                Log.d("DRM", "DRM keys successfully loaded.")
            }

            override fun onDrmSessionReleased(eventTime: AnalyticsListener.EventTime) {
                Log.d("DRM", "DRM session released.")
                mediaInfo.hasDRM = false
                playerViewModel.updateMediaInfo(mediaInfo)
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                Log.i("ExoPlayer", "Dropped $droppedFrames video frames")
            }
        })
    }

    private fun getHighestResolution() : Int {
        val tracks = player.currentTracks
        var lastPixelCount = 0
        var lastHeight = 0
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_VIDEO){
                for (i in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(i)
                    if ((trackFormat.height * trackFormat.width) > lastPixelCount) {
                        lastHeight = trackFormat.height
                        lastPixelCount = trackFormat.height * trackFormat.width
                    }
                }
            }
        }
        return lastHeight
    }

    private fun setSubtitleTheme() {
        val subtitleView: SubtitleView? = binding.playerView.subtitleView

        val foregroundColor = Color.WHITE  // Text color
        val backgroundColor = Color.argb(192, 0, 0, 0)  // Background color
        val windowColor = Color.TRANSPARENT  // Window background

        val customTypeface: Typeface? = ResourcesCompat.getFont(requireContext(), R.font.lato_bold)

        val edgeColor = Color.BLACK
        val captionStyle = CaptionStyleCompat(
            foregroundColor,
            backgroundColor,
            windowColor,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,  // Outline stroke type
            edgeColor,  // Edge color (Black for better contrast)
            customTypeface
        )
        subtitleView?.setStyle(captionStyle)

        subtitleView?.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.25f)

        val params = subtitleView?.layoutParams as? FrameLayout.LayoutParams
        params?.setMargins(30, 30, 30, 30)
        subtitleView?.layoutParams = params
    }

    private fun tryNextStreamSource(channel: ChannelItem, currentStreamSource: StreamSourceItem) {
        Log.i(TAG, "tryNextStreamSource")
        if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
        if (playerViewModel.isChannelLoading.value == false){
            startLoadingIndicatorTimer()
        }
        if (player.isPlaying || player.isLoading) player.stop()
        if (playerViewModel.isSourceForced.value == true){
            loadStreamSource(currentStreamSource)
            playerViewModel.updateTriesCountForEachSource(playerViewModel.triesCountForEachSource.value!! + 1)
            if (playerViewModel.triesCountForEachSource.value!! > TRIES_EACH_SOURCE){
                playerViewModel.hideMediaInfo()
                if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                playerViewModel.showErrorMessage()
                playerViewModel.hideAnimatedLoadingIcon()
                playerViewModel.updateTriesCountForEachSource(0)
            }
        }
        else{
            Log.i(TAG, "triesforeachsource: $playerViewModel.triesCountForEachSource.value")

            val streamSourcesFiltered = channel.streamSources.filter { it.index > currentStreamSource.index }
            var newStreamSourceIndex = streamSourcesFiltered.minByOrNull { it.index }?.index ?: channel.streamSources.minByOrNull { it.index }?.index ?: 0

            val streamSourceMaxIndex = channel.streamSources.maxByOrNull{ it.index }?.index ?: 0
            val streamSourcesCount = channel.streamSources.size
            if (playerViewModel.triesCountForEachSource.value!! == TRIES_EACH_SOURCE){

                if (newStreamSourceIndex > streamSourceMaxIndex) {
                    newStreamSourceIndex = channel.streamSources.minBy { it.index }.index
                }
                val newStreamSource: StreamSourceItem? = channel.streamSources.firstOrNull { it.index == newStreamSourceIndex }
                Log.i(TAG, "newStreamSource:$newStreamSource")
                if (newStreamSource != null) {
                    loadStreamSource(newStreamSource)
                    playerViewModel.updateSourcesTriedCount(playerViewModel.sourcesTriedCount.value!! + 1)
                    Log.i("TryNextStreamSource", "sourcesTriedCount: $playerViewModel.sourcesTriedCount.value")
                    playerViewModel.updateTriesCountForEachSource(1)
                }
                Log.i(TAG, "streamsourcemaxindex $streamSourceMaxIndex")
                Log.i(TAG, "sourcesTriedCount: $playerViewModel.sourcesTriedCount.value")
                Log.i(TAG, "newStreamSourceIndex: $newStreamSourceIndex")
                if (playerViewModel.sourcesTriedCount.value!! > streamSourcesCount) {
                    if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                    playerViewModel.showErrorMessage()
                    playerViewModel.hideAnimatedLoadingIcon()
                }
            }
            else{
                Log.i(TAG, "Load same source: $currentStreamSource")
                loadStreamSource(currentStreamSource)
                playerViewModel.updateTriesCountForEachSource(playerViewModel.triesCountForEachSource.value!! + 1)
            }
        }
    }

    private fun startSourceLoadingTimer() {
        Log.i(TAG, "startLoadingTimer")
        playerViewModel.setIsSourceLoading(true)
        if (!isActivityStopped) handler.postDelayed(sourceLoadingRunnable, SOURCE_LOADING_TIMEOUT_MS)
    }

    private fun startBufferingTimer() {
        Log.i(TAG, "startBufferingTimer")
        playerViewModel.setIsBuffering(true)
        if (!isActivityStopped) handler.postDelayed(bufferingRunnable, BUFFERING_TIMEOUT_MS)
    }

    private fun startCheckPlayingCorrectlyTimer() {
        Log.i(TAG, "startCheckPlayingCorrectlyTimer")
        if (!isActivityStopped) handler.postDelayed(checkPlayingCorrectlyRunnable, PLAYING_TIMEOUT_MS)
    }

    private fun startLoadingIndicatorTimer(){
        Log.i(TAG, "startLoadingIndicatorTimer")
        playerViewModel.setIsChannelLoading(true)
        if (!isActivityStopped) handler.postDelayed(channelLoadingRunnable, CHANNEL_LOADING_TIMEOUT_MS)
    }

    private fun startHidePlayerTimer(){
        Log.i(TAG, "startHidePlayerTimer")
        handler.postDelayed(hidePlayerRunnable, HIDE_PLAYER_TIMEOUT_MS)
    }

    private fun cancelHidePlayerTimer(){
        Log.i(TAG, "cancelHidePlayerTimer")
        handler.removeCallbacks(hidePlayerRunnable)
    }

    private fun cancelCheckPlayingCorrectlyTimer() {
        Log.i(TAG, "cancelCheckPlayingCorrectlyTimer")
        handler.removeCallbacks(checkPlayingCorrectlyRunnable)
    }

    private fun cancelBufferingTimer() {
        Log.i(TAG, "cancelBufferingTimer")
        playerViewModel.setIsBuffering(false)
        handler.removeCallbacks(bufferingRunnable)
    }

    private fun cancelSourceLoadingTimer() {
        Log.i(TAG, "cancelLoadingTimer")

        playerViewModel.setIsSourceLoading(false)
        handler.removeCallbacks(sourceLoadingRunnable)
    }

    private fun cancelLoadingIndicatorTimer(){
        Log.i(TAG, "cancelLoadingIndicatorTimer")

        playerViewModel.setIsChannelLoading(false)
        handler.removeCallbacks(channelLoadingRunnable)
    }

    private fun cancelTryNextStreamSourceTimer() {
        Log.i(TAG, "cancelTryNextStreamSourceTimer")
        handler.removeCallbacks(tryNextStreamSourceRunnable)
    }

    private fun loadChannel(channel: ChannelItem) {
        println("loadChannel")
        if (::jobLoadStreamSource.isInitialized && (jobLoadStreamSource.isActive)) {
            jobLoadStreamSource.cancel()
        }
        if (::jobUIChangeChannel.isInitialized && (jobUIChangeChannel.isActive)) {
            jobUIChangeChannel.cancel()
        }
        if (::jobEPGRender.isInitialized && jobEPGRender.isActive) {
            jobEPGRender.cancel()
        }
        currentNumberInput.clear()
        playerViewModel.hidePlayer()

        if (channel.id < 0) {
            if (binding.channelNumber.isVisible) playerViewModel.hideChannelNumber()
            if (binding.channelName.isVisible) playerViewModel.hideChannelName()
            return
        }
        playerViewModel.hideBottomInfo()
        playerViewModel.hideNumberListMenu()

        if (playerViewModel.isSourceLoading.value == true) cancelSourceLoadingTimer()
        if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
        cancelCheckPlayingCorrectlyTimer()
        cancelTryNextStreamSourceTimer()
        cancelLoadingIndicatorTimer()
        startLoadingIndicatorTimer()

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
        if (channelViewModel.currentCategoryId.value == -1L) {
            playerViewModel.updateChannelNumber(channel.indexFavourite!!)
            channelIdFastSwitch = channel.indexFavourite
        }
        else {
            playerViewModel.updateChannelNumber(channel.indexGroup!!)
            channelIdFastSwitch = channel.indexGroup
        }

        channelViewModel.updateCurrentProgram(null)
        channelViewModel.updateNextProgram(null)


        playerViewModel.updateIsSourceForced(false)
        channelViewModel.updateCurrentChannel(channel)
        showChannelInfoWithTimeout(timeout = TIMEOUT_UI_CHANNEL_LOAD)
        lifecycleScope.launch {
            channelViewModel.updateLastChannelLoaded(channel.id)
            channelViewModel.updateLastCategoryLoaded(channelViewModel.currentCategoryId.value ?: -1L)
        }

        val streamSources = channel.streamSources
        Log.i(TAG, streamSources.toString())
        if (streamSources.isNotEmpty()) {
            loadStreamSource(streamSources.minBy { it.index })
            playerViewModel.updateTriesCountForEachSource(1)
            playerViewModel.updateSourcesTriedCount(1)
        }
        else{
            println("No stream sources found for channel: $channel")
        }
    }

    private fun loadStreamSource(streamSource: StreamSourceItem) {
        if (::jobLoadStreamSource.isInitialized && (jobLoadStreamSource.isActive)) {
            jobLoadStreamSource.cancel()
        }
        if (playerViewModel.isSourceLoading.value == true) cancelSourceLoadingTimer()
        if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
        cancelCheckPlayingCorrectlyTimer()
        cancelTryNextStreamSourceTimer()

        if (playerViewModel.currentStreamSource.value?.id != streamSource.id) {
            currentAudioTrack = null
            currentVideoTrack = null
            currentSubtitlesTrack = null
            playerViewModel.updateIsQualityForced(false)
            playerViewModel.hidePlayer()
        }

        if (player.isLoading || player.isPlaying) {
            player.stop()
        }

        if (playerViewModel.isSourceForced.value == true && playerViewModel.isChannelLoading.value == false){
            cancelLoadingIndicatorTimer()
            startLoadingIndicatorTimer()
        }

        startSourceLoadingTimer()
        resetMediaInfo()
        if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
        playerViewModel.updateCurrentStreamSource(streamSource)
        jobLoadStreamSource = CoroutineScope(Dispatchers.IO).launch {
            delay(250)

            val cachedUrlObj = urlCache[streamSource]
            val currentTime = System.currentTimeMillis()

            if (streamSource.proxies != null) {
                if (streamSource.proxies.isEmpty()) {
                    Log.i(TAG, "No proxies, reset")
                    ProxySelector.setDefault(originalProxySelector)
                }
                else{
                    Log.i(TAG, "Found proxy: ${streamSource.proxies.first()}")
                    val proxy = streamSource.proxies.first()
                    ProxySelector.setDefault(CustomProxySelector(proxy.hostname, proxy.port))
                }
            }

            val url = if (cachedUrlObj != null && (currentTime - cachedUrlObj.timestamp) < cacheExpirationTime) {
                cachedUrlObj.url
            } else {
                val newUrl = ApiService.getURLFromChannelSource(streamSource)!!
                urlCache[streamSource] = CachedUrl(newUrl, currentTime)
                newUrl
            }
            ensureActive()

            val headersObj: List<StreamSourceHeaderItem> = streamSource.headers ?: emptyList()
            val headers : MutableMap<String, String> = when (streamSource.streamSourceType) {
                StreamSourceTypeItem.IPTV -> {
                    ApiService.getHeadersMapFromHeadersObject(headersObj)
                }
                StreamSourceTypeItem.TWITCH -> {
                    mapOf("User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0",
                        "Accept" to "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain",
                        "Accept-Language" to "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3",
                        "Accept-Encoding" to "gzip, deflate, br, zstd" ,
                        "Referer" to "https://www.twitch.tv/",
                        "Origin" to "https://www.twitch.tv",
                        "Connection" to "keep-alive",
                        "Sec-Fetch-Dest" to "empty",
                        "Sec-Fetch-Mode" to "cors",
                        "Sec-Fetch-Site" to "cross-site",
                        "Priority" to "u=4")
                }

                else -> {
                    mapOf()
                }
            }.toMutableMap()

            if (headers["User-Agent"] == null) {
                headers["User-Agent"] = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
            }

            Log.i(TAG,"Stream URL: $url")
            Log.i(TAG,"Headers: $headers")

            val cronetEngine = CronetEngine.Builder(requireContext()).build()
            val dataSourceFactory = if (url.startsWith("http:") || url.startsWith("https:")) {
                CronetDataSource.Factory(cronetEngine, MoreExecutors.directExecutor()).apply {
                    setDefaultRequestProperties(headers)
                }
            }
            else if (url.startsWith("rtmp:")) {
                RtmpDataSource.Factory()
            } else {
                CronetDataSource.Factory(cronetEngine, MoreExecutors.directExecutor()).apply {
                    setDefaultRequestProperties(headers)
                }
            }

            val mediaSource = if (url.contains(".m3u8")) {
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(url.toUri()))
            }
            else if (url.contains(".mpd") && streamSource.drmType != DrmTypeItem.NONE) {
                if (streamSource.drmType == DrmTypeItem.LICENSE) {
                    if (streamSource.useUnofficialDrmLicenseMethod) {
                        val apiResponse = ApiService.getDrmKeys(streamSource)
                        val drmBody = MediaUtils.generateDrmBodyFromApiResponse(apiResponse)
                        Log.i("DRM", drmBody)

                        val dashMediaItem = MediaItem.Builder()
                            .setUri(url.toUri())
                            .setMimeType(MimeTypes.APPLICATION_MPD)
                            .setMediaMetadata(MediaMetadata.Builder().setTitle("test").build())
                            .build()

                        val drmCallback = LocalMediaDrmCallback(drmBody.toByteArray())
                        val drmSessionManager = DefaultDrmSessionManager.Builder()
                            .setPlayClearSamplesWithoutKeys(true)
                            .setMultiSession(false)
                            .setKeyRequestParameters(HashMap())
                            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                            .build(drmCallback)

                        val customDrmSessionManager: DrmSessionManager = drmSessionManager
                        val mediaSourceFactory = DashMediaSource.Factory(dataSourceFactory)
                            .setDrmSessionManagerProvider { customDrmSessionManager }
                            .createMediaSource(dashMediaItem)
                        mediaSourceFactory
                    }
                    else{
                        val mediaSourceFactory = DashMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(
                                MediaItem.Builder().setUri(url.toUri())
                                    .setDrmConfiguration(
                                        MediaItem.DrmConfiguration
                                            .Builder(C.WIDEVINE_UUID)
                                            .setLicenseUri("https://drmnew.tvup.cloud/license/SAT53")
                                            .setLicenseRequestHeaders(
                                                mapOf(
                                                    "Origin" to "https://www.tivify.tv",
                                                    "Referer" to "https://www.tivify.tv/",
                                                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                                                    "X-Access-Token" to "eyJ1c2VySWQiOiJwdXJjaGFzZSIsInNlc3Npb25JZCI6InAwIiwibWVyY2hhbnQiOiJtb2dfcnRwIn0=",
                                                    "Accept-Encoding" to "gzip, deflate, br, zstd",
                                                    "Content-Type" to "application/octet-stream",
                                                    "X-Tvup-Device" to "2369bbed-45fd-48c6-9294-a6d9341df003",
                                                )
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                        mediaSourceFactory
                    }

                } else {
                    val drmKeys = streamSource.drmKeys ?: ""
                    println("drmKeys: $drmKeys")
                    println("drmkeys: ${streamSource.drmKeys}")
                    val drmBody = MediaUtils.generateDrmBodyFromKeys(drmKeys)
                    Log.i("DRM", drmBody)

                    val dashMediaItem = MediaItem.Builder()
                        .setUri(url.toUri())
                        .setMimeType(MimeTypes.APPLICATION_MPD)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle("test").build())
                        .build()

                    val drmCallback = LocalMediaDrmCallback(drmBody.toByteArray())
                    val drmSessionManager = DefaultDrmSessionManager.Builder()
                        .setPlayClearSamplesWithoutKeys(true)
                        .setMultiSession(false)
                        .setKeyRequestParameters(HashMap())
                        .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                        .build(drmCallback)

                    val customDrmSessionManager: DrmSessionManager = drmSessionManager
                    val mediaSourceFactory = DashMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManagerProvider { customDrmSessionManager }
                        .createMediaSource(dashMediaItem)
                    mediaSourceFactory
                }
            }
            else if (url.contains(".mpd")) {
                Log.i("DRM", "NONE")
                DashMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(url.toUri()))
            }
            else if (url.contains("http://ytproxy")) {
                val mediaItem = MediaItem.Builder()
                    .setUri(url.toUri())
                    .setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                            .build()
                    )
                    .build()
                DashMediaSource.Factory(dataSourceFactory)
                    .setManifestParser(LiveDashManifestParser())
                    .createMediaSource(mediaItem)
            }
            else {
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(
                    url.toUri()))
            }

            ensureActive()

            activity?.runOnUiThread{
                player.setMediaSource(mediaSource)

                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverrides()
                    .build()
                player.prepare()
                player.play()
            }
        }
    }

    private fun loadSubtitlesTrack(subtitlesTrack: SubtitlesTrack){
        if (subtitlesTrack.id == "-1") {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverrides()
                .build()
            currentSubtitlesTrack = null
            return
        }
        val tracks = player.currentTracks
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(i)
                    if (subtitlesTrack.id == trackFormat.id) {
                        player.trackSelectionParameters =
                            player.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                                .build()
                        currentSubtitlesTrack = subtitlesTrack
                    }
                }
            }
        }
    }

    private fun loadAudioTrack(audioTrack: AudioTrack) {
        val tracks = player.currentTracks
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_AUDIO){
                for (i in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(i)
                    if (audioTrack.id == trackFormat.id) {
                        player.trackSelectionParameters =
                            player.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                                .build()
                        currentAudioTrack = audioTrack
                    }
                }
            }
        }
    }

    private fun loadVideoTrack(videoTrack: VideoTrack) {
        if (videoTrack.id == "-1") {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverrides()
                .build()
            currentVideoTrack = null
            return
        }
        val tracks = player.currentTracks
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_VIDEO){
                for (i in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(i)
                    if (videoTrack.id == trackFormat.id) {
                        player.trackSelectionParameters =
                            player.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                                .build()
                        currentVideoTrack = videoTrack
                    }
                }
            }
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

    /* This will need to be refactored */
    private fun showChannelInfoWithTimeout(timeout: Long){
        if (::jobUITimeout.isInitialized && jobUITimeout.isActive) {
            jobUITimeout.cancel()
        }
        if (::jobEPGRender.isInitialized && jobEPGRender.isActive) {
            jobEPGRender.cancel()
        }
        jobEPGRender = lifecycleScope.launch {
            val currentChannel = channelViewModel.currentChannel.value
            if (currentChannel != null){
                channelViewModel.updateCurrentProgramForChannel(currentChannel.id)
            }

        }
        jobUITimeout = lifecycleScope.launch {
            try{
                activity?.runOnUiThread {
                    if (!isAndroidTV(requireContext())) {
                        playerViewModel.showButtonUp()
                        playerViewModel.showButtonDown()
                        playerViewModel.showButtonChannelList()
                        playerViewModel.showButtonSettings()
                        playerViewModel.showButtonPiP()
                        playerViewModel.showButtonCategoryList()
                    }
                    playerViewModel.hideTimeDate()
                    playerViewModel.showChannelNumber()
                    if (channelViewModel.currentCategoryId.value != -1L) {
                        Log.i("PlayerActivity", "showChannelNumberCategory: ${channelViewModel.currentCategoryId.value}")
                        playerViewModel.showCategoryName()
                    }
                    playerViewModel.showChannelName()
                    playerViewModel.showBottomInfo()
                    if (playerViewModel.isSourceLoading.value == false) {
                        playerViewModel.showMediaInfo()
                    }
                }
                delay(timeout)
                ensureActive()
                activity?.runOnUiThread {
                    if (!isAndroidTV(requireContext())) {
                        playerViewModel.hideButtonUp()
                        playerViewModel.hideButtonDown()
                        playerViewModel.hideButtonChannelList()
                        playerViewModel.hideButtonSettings()
                        playerViewModel.hideButtonPiP()
                        playerViewModel.hideButtonCategoryList()
                    }
                    if (!isLongPressDown && !isLongPressUp) {
                        playerViewModel.hideChannelNumber()
                    }
                    playerViewModel.hideChannelName()
                    playerViewModel.hideCategoryName()
                    playerViewModel.hideTimeDate()
                    playerViewModel.hideMediaInfo()
                    playerViewModel.hideBottomInfo()
                }
            } catch (_: CancellationException){
            }
        }
    }

    /* This will need to be refactored */
    private fun showFullChannelUIWithTimeout(timeout: Long = 4000){
        if (::jobUITimeout.isInitialized && jobUITimeout.isActive) {
            jobUITimeout.cancel()
        }
        if (::jobEPGRender.isInitialized && jobEPGRender.isActive) {
            jobEPGRender.cancel()
        }
        jobEPGRender = lifecycleScope.launch {
            val currentChannel = channelViewModel.currentChannel.value
            if (currentChannel != null){
                delay(100)
                channelViewModel.updateCurrentProgramForChannel(currentChannel.id)
            }
        }
        jobUITimeout = lifecycleScope.launch {
            try{
                activity?.runOnUiThread {
                    if (!isAndroidTV(requireContext())) {
                        playerViewModel.showButtonUp()
                        playerViewModel.showButtonDown()
                        playerViewModel.showButtonChannelList()
                        playerViewModel.showButtonSettings()
                        playerViewModel.showButtonPiP()
                        playerViewModel.showButtonCategoryList()
                    }
                    playerViewModel.showChannelNumber()
                    if (channelViewModel.currentCategoryId.value != -1L) {
                        playerViewModel.showCategoryName()
                    }
                    playerViewModel.showChannelName()
                    playerViewModel.updateTimeDate()
                    playerViewModel.showTimeDate()
                    if (playerViewModel.isSourceLoading.value == false) {
                        playerViewModel.showMediaInfo()
                    }
                    if (channelViewModel.currentProgram.value != null || channelViewModel.nextProgram.value != null) {
                        playerViewModel.showBottomInfo()
                    }
                }
                delay(timeout)
                ensureActive()
                activity?.runOnUiThread {
                    if (!isAndroidTV(requireContext())) {
                        playerViewModel.hideButtonUp()
                        playerViewModel.hideButtonDown()
                        playerViewModel.hideButtonChannelList()
                        playerViewModel.hideButtonSettings()
                        playerViewModel.hideButtonPiP()
                        playerViewModel.hideButtonCategoryList()
                    }
                    if (!isLongPressDown && !isLongPressUp){
                        playerViewModel.hideChannelNumber()
                    }
                    playerViewModel.hideChannelName()
                    playerViewModel.hideCategoryName()
                    playerViewModel.hideTimeDate()
                    playerViewModel.hideMediaInfo()
                    playerViewModel.hideBottomInfo()
                }
            } catch (_: CancellationException){
            }
        }
    }

    /* This will need to be refactored */
    private fun showChannelNumberWithTimeoutAndChangeChannel(timeout: Long = 3000){
        if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) {
            jobUIChangeChannel.cancel()
        }
        jobUIChangeChannel =
        lifecycleScope.launch {
            try{
                activity?.runOnUiThread {
                    playerViewModel.showChannelNumberKeyboard()
                    playerViewModel.hideBottomInfo()
                }
                delay(timeout)
                ensureActive()
                channelViewModel.updateIsLoadingChannel(true)
                val newChannel = channelViewModel.getChannel(categoryId = -1L, currentNumberInput.toString().toInt())
                playerViewModel.hideChannelNumberKeyboard()
                if (newChannel.id != channelViewModel.currentChannel.value?.id) {
                    channelViewModel.updateCurrentCategoryId(-1L)
                    playerViewModel.updateCategoryName("Favoritos")
                    currentNumberInput.clear()
                    channelViewModel.updateLastCategoryLoaded(-1L)
                    channelViewModel.updateIsLoadingChannel(false)
                    loadChannel(newChannel)
                }
                else{
                    currentNumberInput.clear()
                    channelViewModel.updateIsLoadingChannel(false)
                    showChannelInfoWithTimeout(TIMEOUT_UI_CHANNEL_LOAD)
                }

            } catch (_: CancellationException){
                playerViewModel.hideChannelNumberKeyboard()
                channelViewModel.updateIsLoadingChannel(false)
            } catch (_: ChannelNotFoundException){
                playerViewModel.hideChannelNumberKeyboard()
                currentNumberInput.clear()
                channelViewModel.updateIsLoadingChannel(false)
            } catch (_: NumberFormatException){
                playerViewModel.hideChannelNumberKeyboard()
                currentNumberInput.clear()
                channelViewModel.updateIsLoadingChannel(false)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (channelViewModel.isLoadingChannelList.value == true
            || channelViewModel.isImportingData.value == true
            || channelViewModel.isLoadingChannel.value == true) return true
        val code = event.keyCode
        val action = event.action
        val isDown = action == 0

        if (!isDown){
            if (isLongPressDown || isLongPressUp){
                channelViewModel.updateIsLoadingChannel(true)
                if (::jobFastChangeChannel.isInitialized && jobFastChangeChannel.isActive){
                    lifecycleScope.launch {
                        jobFastChangeChannel.cancelAndJoin()
                    }
                }
                isLongPressUp = false
                isLongPressDown = false
                if (channelViewModel.currentCategoryId.value == -1L){
                    jobFastChangeChannel = lifecycleScope.launch {
                        Log.i(TAG, "After pressing long button: get newChannelId: $channelIdFastSwitch")
                        val newChannel = channelViewModel.getNextChannel(-1L, channelIdFastSwitch)
                        Log.i(TAG, "After pressing long button: load newChannelId: $channelIdFastSwitch")
                        channelViewModel.updateIsLoadingChannel(false)
                        loadChannel(newChannel)
                    }
                }
                else{
                    jobFastChangeChannel = lifecycleScope.launch {
                        val newChannel = channelViewModel.getNextChannel(channelViewModel.currentCategoryId.value!!, channelIdFastSwitch)
                        loadChannel(newChannel)
                        channelViewModel.updateIsLoadingChannel(false)
                    }
                }
            }
            else{
                return false
            }
        }
        else{
            when (code) {
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    if (isLongPressDown || isLongPressUp) return true
                    // Load channel from channel list
                    if (playerViewModel.isChannelListVisible.value == true) {
                        val currentChannelIndex = if (channelViewModel.currentCategoryId.value == -1L) {
                            channelViewModel.currentChannel.value?.indexFavourite!!
                        } else {
                            channelViewModel.currentChannel.value?.indexGroup!!
                        }
                        Log.i(TAG, "dispatchKeyEvent: currentChannelIndex: $currentChannelIndex")
                        val focusedView = rvChannelList.focusedChild
                        if (focusedView != null) {
                            val position = rvChannelList.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            Log.i(
                                TAG,
                                "dispatchKeyEvent: currentItemSelectedFromChannelList: $position"
                            )
                            val newChannelSm =
                                (rvChannelList.adapter as? ChannelListAdapter)?.getItemAtPosition(
                                    position
                                ) as ChannelItem
                            var newChannel: ChannelItem?
                            if (channelViewModel.currentCategoryId.value == -1L) {
                                lifecycleScope.launch {
                                    newChannel = channelViewModel.getNextChannel(
                                        -1L,
                                        newChannelSm.indexFavourite!!
                                    )
                                    newChannel?.let { loadChannel(it) }
                                }
                            } else {
                                println("newChannelSm.indexFavourite: ${newChannelSm.indexFavourite}, channelViewModel.currentCategoryId.value: ${channelViewModel.currentCategoryId.value}")
                                lifecycleScope.launch {
                                    newChannel = channelViewModel.getNextChannel(
                                        channelViewModel.currentCategoryId.value!!,
                                        newChannelSm.indexGroup!!
                                    )
                                    newChannel?.let { loadChannel(it) }
                                }
                            }
                        }
                        else{
                            channelViewModel.updateIsLoadingChannel(false)
                            showChannelInfoWithTimeout(TIMEOUT_UI_CHANNEL_LOAD)
                        }
                    }
                    else if (playerViewModel.isCategoryListVisible.value == true) {
                        channelViewModel.updateIsLoadingChannel(true)
                        val focusedView = rvCategoryList.focusedChild
                        if (focusedView != null) {
                            val position = rvCategoryList.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val newCategory = (rvCategoryList.adapter as? CategoryListAdapter)?.getItemAtPosition(position) as CategoryItem
                            channelViewModel.updateCurrentCategoryId(newCategory.id)
                            playerViewModel.updateCategoryName(newCategory.name)
                            lifecycleScope.launch {
                                loadChannel(channelViewModel.getNextChannel(categoryId = newCategory.id, groupId = 1))
                                channelViewModel.updateIsLoadingChannel(false)
                                Log.i(TAG, "dispatchKeyEvent: newCategory: $newCategory")
                                channelViewModel.updateLastCategoryLoaded(newCategory.id)
                            }

                            playerViewModel.hideChannelName()
                            playerViewModel.hideCategoryName()
                            playerViewModel.hideChannelNumber()
                            playerViewModel.hideBottomInfo()
                            playerViewModel.hideMediaInfo()
                            playerViewModel.hideTimeDate()

                            playerViewModel.hideCategoryList()
                        }
                        else{
                            channelViewModel.updateIsLoadingChannel(false)
                            playerViewModel.hideCategoryList()
                        }
                    }
                    // Enter specific setting
                    else if (playerViewModel.isSettingsMenuVisible.value == true) {
                        val focusedView = rvChannelSettings.focusedChild
                        if (focusedView != null) {
                            val position = rvChannelSettings.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            playerViewModel.hideSettingsMenu()

                            when (position) {
                                ChannelSettings.AUDIO_TRACKS -> {
                                    loadSetting(ChannelSettings.AUDIO_TRACKS)
                                }
                                ChannelSettings.SUBTITLES_TRACKS -> {
                                    loadSetting(ChannelSettings.SUBTITLES_TRACKS)
                                }
                                ChannelSettings.SOURCES -> {
                                    loadSetting(ChannelSettings.SOURCES)
                                }
                                ChannelSettings.VIDEO_TRACKS -> {
                                    loadSetting(ChannelSettings.VIDEO_TRACKS)
                                }
                                ChannelSettings.UPDATE_EPG -> {
                                    loadSetting(ChannelSettings.UPDATE_EPG)
                                }
                                ChannelSettings.SHOW_EPG -> {
                                    loadSetting(ChannelSettings.SHOW_EPG)
                                }
                                ChannelSettings.UPDATE_CHANNEL_LIST -> {
                                    loadSetting(ChannelSettings.UPDATE_CHANNEL_LIST)
                                }
                            }
                        }
                        else{
                            playerViewModel.hideSettingsMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.AUDIO_TRACKS) {
                        val focusedView = rvAudioTracks.focusedChild
                        if (focusedView != null) {
                            val position = rvAudioTracks.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val audioTrack = (rvAudioTracks.adapter as? AudioTracksAdapter)?.getItemAtPosition(position) as AudioTrack
                            loadAudioTrack(audioTrack)
                            playerViewModel.hideTrackMenu()
                        }
                        else{
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.SUBTITLES_TRACKS) {
                        val focusedView = rvSubtitlesTracks.focusedChild
                        if (focusedView != null) {
                            val position = rvSubtitlesTracks.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val subtitlesTrack = (rvSubtitlesTracks.adapter as? SubtitlesTracksAdapter)?.getItemAtPosition(position) as SubtitlesTrack
                            loadSubtitlesTrack(subtitlesTrack)
                            playerViewModel.hideTrackMenu()
                        }
                        else{
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.SOURCES) {
                        val focusedView = rvChannelSources.focusedChild
                        if (focusedView != null) {
                            val position = rvChannelSources.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            if (playerViewModel.isErrorMessageVisible.value == true) {
                                playerViewModel.hideErrorMessage()
                            }
                            playerViewModel.hideBottomErrorMessage()
                            var streamSource = (rvChannelSources.adapter as? ChannelSourcesAdapter)?.getItemAtPosition(position) as StreamSourceItem
                            if (streamSource.id.toInt() == -1) {
                                playerViewModel.updateIsSourceForced(false)
                                val currentChannel = channelViewModel.currentChannel.value
                                if (currentChannel != null) {
                                    streamSource = currentChannel.streamSources.minBy { it.index }
                                }
                            }
                            else{
                                playerViewModel.updateIsSourceForced(true)
                            }
                            playerViewModel.updateTriesCountForEachSource(1)
                            playerViewModel.updateSourcesTriedCount(1)
                            playerViewModel.hideAnimatedLoadingIcon()
                            loadStreamSource(streamSource)
                            playerViewModel.hideTrackMenu()
                            playerViewModel.hidePlayer()
                        }
                        else{
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.VIDEO_TRACKS) {
                        val focusedView = rvChannelSources.focusedChild
                        if (focusedView != null) {
                            val position = rvChannelSources.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val videoTrack = (rvVideoTracks.adapter as? VideoTracksAdapter)?.getItemAtPosition(position) as VideoTrack
                            try{
                                if (videoTrack.id.toInt() == -1) {
                                    playerViewModel.updateIsQualityForced(false)
                                }
                                else{
                                    playerViewModel.updateIsQualityForced(true)
                                }
                            }
                            catch (_: Exception) {}
                            loadVideoTrack(videoTrack)
                            playerViewModel.hideTrackMenu()
                        }
                        else{
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isNumberListMenuVisible.value == true) {
                        if (event.repeatCount > 0) {
                            return true
                        }
                        if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) {
                            jobUIChangeChannel.cancel()
                        }
                        if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                        if (playerViewModel.isChannelNameVisible.value == true) playerViewModel.hideChannelName()
                        if (playerViewModel.isChannelNumberVisible.value == true) playerViewModel.hideChannelNumber()
                        if (playerViewModel.isCategoryNameVisible.value == true) playerViewModel.hideCategoryName()
                        if (currentNumberInput.length >= MAX_DIGITS) {
                            currentNumberInput.clear()
                        }

                        val focusedView = rvNumberList.focusedChild
                        if (focusedView != null) {
                            val position = rvNumberList.getChildAdapterPosition(focusedView)
                            println("Focused Item Position: $position")
                            val selectedNumber = (rvNumberList.adapter as? NumberListAdapter)?.getItemAtPosition(position) as Int
                            val number = selectedNumber.toString()

                            currentNumberInput.append(number)

                            binding.channelNumberKeyboard.text = currentNumberInput.toString()

                            showChannelNumberWithTimeoutAndChangeChannel(3000)

                        }
                        else{
                            playerViewModel.hideNumberListMenu()
                        }
                    }
                    else if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                        channelViewModel.updateIsLoadingChannel(true)
                        if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) jobUIChangeChannel.cancel()
                        val channelIndex = currentNumberInput.toString().toInt()
                        playerViewModel.hideChannelNumberKeyboard()
                        currentNumberInput.clear()
                        lifecycleScope.launch {
                            try{
                                channelViewModel.updateIsLoadingChannel(true)
                                val newChannel = channelViewModel.getChannel(
                                    -1L,
                                    channelIndex
                                )
                                channelViewModel.updateCurrentCategoryId(-1L)
                                playerViewModel.updateCategoryName("Favoritos")
                                channelViewModel.updateLastCategoryLoaded(-1L)
                                if (channelViewModel.currentChannel.value?.id != newChannel.id) {
                                    loadChannel(newChannel)
                                }
                                else{
                                    showChannelInfoWithTimeout(TIMEOUT_UI_CHANNEL_LOAD)
                                }
                                currentNumberInput.clear()
                                channelViewModel.updateIsLoadingChannel(false)
                            } catch (_: ChannelNotFoundException) {
                                playerViewModel.hideChannelNumberKeyboard()
                                currentNumberInput.clear()
                                channelViewModel.updateIsLoadingChannel(false)
                            } catch (_: NumberFormatException) {
                                playerViewModel.hideChannelNumberKeyboard()
                                currentNumberInput.clear()
                                channelViewModel.updateIsLoadingChannel(false)
                            }
                        }
                    }
                    else if (playerViewModel.isChannelNumberVisible.value == true) {
                        if (event.repeatCount > 0) {
                            Log.i(TAG, "event repeat center")
                            if (playerViewModel.isNumberListMenuVisible.value != true) {
                                Log.i(TAG, "showNumberListMenu")
                                playerViewModel.hideMediaInfo()
                                playerViewModel.hideChannelName()
                                playerViewModel.hideChannelNumber()
                                playerViewModel.hideCategoryName()
                                playerViewModel.hideTimeDate()
                                playerViewModel.hideBottomInfo()
                                playerViewModel.showNumberListMenu()
                                rvNumberList.requestFocus()
                            }
                            return true
                        }
                        else{
                            if (!isAndroidTV(requireContext())) {
                                playerViewModel.hideButtonUp()
                                playerViewModel.hideButtonDown()
                                playerViewModel.hideButtonChannelList()
                                playerViewModel.hideButtonSettings()
                                playerViewModel.hideButtonPiP()
                                playerViewModel.hideButtonCategoryList()
                            }
                            playerViewModel.hideChannelNumber()
                            playerViewModel.hideChannelName()
                            playerViewModel.hideCategoryName()
                            playerViewModel.hideTimeDate()
                            playerViewModel.hideMediaInfo()
                            playerViewModel.hideBottomInfo()
                        }
                        return true
                    }
                    // Show current channel info
                    else{
                        if (event.repeatCount > 0) {
                            Log.i(TAG, "event repeat center")
                            if (playerViewModel.isNumberListMenuVisible.value != true) {
                                Log.i(TAG, "showNumberListMenu")
                                playerViewModel.hideMediaInfo()
                                playerViewModel.hideChannelName()
                                playerViewModel.hideChannelNumber()
                                playerViewModel.hideCategoryName()
                                playerViewModel.hideTimeDate()
                                playerViewModel.hideBottomInfo()
                                playerViewModel.showNumberListMenu()
                                rvNumberList.requestFocus()
                            }
                            return true
                        }
                        else{
                            if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) {
                                jobUIChangeChannel.cancel()
                            }
                            if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                                playerViewModel.hideChannelNumberKeyboard()
                                currentNumberInput.clear()
                            }
                            showFullChannelUIWithTimeout(timeout = TIMEOUT_UI_INFO)
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (binding.channelList.isVisible
                        || binding.rvChannelSettings.isVisible
                        || binding.rvCategoryList.isVisible) { // Navigate through menu
                        return false
                    }
                    else if (binding.rvChannelTrackSettings.isVisible) {
                        return rvAudioTracks.adapter!!.itemCount == 0 || rvSubtitlesTracks.adapter!!.itemCount == 0
                    }
                    else if (binding.rvNumberList.isVisible) {
                        return true
                    } else{
                        if (event.repeatCount > 0){
                            //if (isLongPressDown) return true // Uncomment to disallow 'channel fast switch direction' change
                            isLongPressUp = true
                            if (channelViewModel.currentCategoryId.value == -1L) {
                                binding.channelNumber.visibility = View.VISIBLE
                                binding.channelName.visibility = View.INVISIBLE
                                binding.channelMediaInfo.visibility = View.GONE

                                jobFastChangeChannel = lifecycleScope.launch {
                                    if (::jobFastChangeChannel.isInitialized && jobFastChangeChannel.isActive) jobFastChangeChannel.cancelAndJoin()
                                    println("newChannelIndex before: $channelIdFastSwitch")
                                    channelIdFastSwitch = channelViewModel.getNextChannelIndex(-1L, channelIdFastSwitch)
                                    println("newChannelIndex after: $channelIdFastSwitch")
                                    binding.channelNumber.text = (channelIdFastSwitch).toString()
                                }
                            }
                            else{
                                binding.channelNumber.visibility = View.VISIBLE
                                binding.channelName.visibility = View.INVISIBLE
                                binding.channelMediaInfo.visibility = View.GONE

                                jobFastChangeChannel = lifecycleScope.launch {
                                    if (::jobFastChangeChannel.isInitialized && jobFastChangeChannel.isActive) jobFastChangeChannel.cancelAndJoin()
                                    println("newChannelIndex before: $channelIdFastSwitch")
                                    channelIdFastSwitch = channelViewModel.getNextChannelIndex(channelViewModel.currentCategoryId.value!!, channelIdFastSwitch)
                                    println("newChannelIndex after: $channelIdFastSwitch")
                                    binding.channelNumber.text = (channelIdFastSwitch).toString()
                                }
                            }
                        }
                        else{
                            if (!isLongPressDown){
                                if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                                    playerViewModel.hideChannelNumberKeyboard()
                                    if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive){
                                        jobUIChangeChannel.cancel()
                                    }
                                }
                                loadNextChannel()
                            }
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (binding.channelList.isVisible
                        || binding.rvChannelSettings.isVisible
                        || binding.rvCategoryList.isVisible) { // Navigate through menu
                        return false
                    } else if (binding.rvChannelTrackSettings.isVisible) {
                        return rvAudioTracks.adapter!!.itemCount == 0 || rvSubtitlesTracks.adapter!!.itemCount == 0
                    }
                    else if (binding.rvNumberList.isVisible) {
                        return true
                    }
                    else{ // Change to previous channel
                        if (event.repeatCount > 0) {
                            //if (isLongPressUp) return true // Uncomment to disallow 'channel fast switch direction' change
                            isLongPressDown = true
                            if (channelViewModel.currentCategoryId.value == -1L) {
                                binding.channelNumber.visibility = View.VISIBLE
                                binding.channelName.visibility = View.INVISIBLE
                                binding.channelMediaInfo.visibility = View.GONE

                                jobFastChangeChannel = lifecycleScope.launch {
                                        println("newChannelIndex before: $channelIdFastSwitch")
                                        channelIdFastSwitch = channelViewModel.getPreviousChannelIndex(-1L, channelIdFastSwitch)
                                        println("newChannelIndex after: $channelIdFastSwitch")
                                        binding.channelNumber.text = (channelIdFastSwitch).toString()
                                }
                            }
                            else{
                                binding.channelNumber.visibility = View.VISIBLE
                                binding.channelName.visibility = View.INVISIBLE
                                binding.channelMediaInfo.visibility = View.GONE
                                jobFastChangeChannel = lifecycleScope.launch {
                                    if (::jobFastChangeChannel.isInitialized && jobFastChangeChannel.isActive) jobFastChangeChannel.cancelAndJoin()
                                    println("newChannelIndex before: $channelIdFastSwitch")
                                    channelIdFastSwitch = channelViewModel.getPreviousChannelIndex(channelViewModel.currentCategoryId.value!!, channelIdFastSwitch)
                                    println("newChannelIndex after: $channelIdFastSwitch")
                                    binding.channelNumber.text = (channelIdFastSwitch).toString()
                                }
                            }

                        }
                        else{
                            if (!isLongPressUp){
                                if (playerViewModel.isChannelNumberKeyboardVisible.value == true) {
                                    playerViewModel.hideChannelNumberKeyboard()
                                    if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive){
                                        jobUIChangeChannel.cancel()
                                    }
                                }
                                loadPreviousChannel()
                            }
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (binding.rvChannelSettings.isVisible) {
                        if (event.repeatCount > 0) return true
                        playerViewModel.hideSettingsMenu()
                        return true
                    }
                    else if (binding.channelList.isVisible
                        || binding.rvCategoryList.isVisible
                        || binding.rvChannelTrackSettings.isVisible) {
                        return true
                    }
                    else if (binding.rvNumberList.isVisible) {
                        return false
                    }
                    else{
                        if (event.repeatCount > 0) return true
                        initSettingsMenu()
                        playerViewModel.showSettingsMenu()
                        rvChannelSettings.requestFocus()
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (binding.rvChannelTrackSettings.isVisible
                        || binding.rvChannelSettings.isVisible
                        || binding.channelList.isVisible
                    ) {
                        return true
                    } else if (binding.rvNumberList.isVisible) {
                        return false
                    } else if (binding.rvCategoryList.visibility != View.VISIBLE) {
                        if (event.repeatCount > 0) return true
                        playerViewModel.showCategoryList()
                        rvCategoryList.requestFocus()
                        return true
                    }
                    else if (binding.rvCategoryList.isVisible) {
                        if (event.repeatCount > 0) return true
                        playerViewModel.hideCategoryList()
                    }
                }

                (KeyEvent.KEYCODE_MENU) -> {
                    if (event.repeatCount > 0) return true
                    if (binding.channelList.isVisible) {
                        playerViewModel.hideChannelList()
                        return true
                    }
                    else if (playerViewModel.isSettingsMenuVisible.value == true) {
                        playerViewModel.hideSettingsMenu()
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true) {
                        playerViewModel.hideTrackMenu()
                    }
                    else if (playerViewModel.isCategoryListVisible.value == true) {
                        playerViewModel.hideCategoryList()
                    }
                    lifecycleScope.launch {
                        initChannelList()
                        rvChannelList.scrollToPosition(playerViewModel.currentItemSelectedFromChannelList.value!!)
                        playerViewModel.showChannelList()
                        rvChannelList.requestFocus()
                    }
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (playerViewModel.isChannelListVisible.value == true) {
                        playerViewModel.hideChannelList()
                        return true
                    }
                    else if (playerViewModel.isSettingsMenuVisible.value == true) {
                        playerViewModel.hideSettingsMenu()
                        return true
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true) {
                        playerViewModel.hideTrackMenu()
                        return true
                    }
                    else if (playerViewModel.isCategoryListVisible.value == true) {
                        playerViewModel.hideCategoryList()
                        return true
                    }
                    else if (playerViewModel.isNumberListMenuVisible.value == true) {
                        playerViewModel.hideNumberListMenu()
                        return true
                    }
                    else{
                        ProxySelector.setDefault(originalProxySelector)
                        return false
                    }
                }

                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                    if (isLongPressUp || isLongPressDown) return true
                    if (::jobUIChangeChannel.isInitialized && jobUIChangeChannel.isActive) {
                        jobUIChangeChannel.cancel()
                    }
                    if (binding.channelList.isVisible) return true
                    if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                    if (playerViewModel.isChannelNameVisible.value == true) playerViewModel.hideChannelName()
                    if (playerViewModel.isChannelNumberVisible.value == true) playerViewModel.hideChannelNumber()
                    if (playerViewModel.isCategoryNameVisible.value == true) playerViewModel.hideCategoryName()
                    if (currentNumberInput.length >= MAX_DIGITS) {
                        currentNumberInput.clear()
                    }

                    val number = (code - KeyEvent.KEYCODE_0).toString()

                    currentNumberInput.append(number)

                    binding.channelNumberKeyboard.text = currentNumberInput.toString()

                    showChannelNumberWithTimeoutAndChangeChannel(3000)

                    return true
                }
            }
        }
        return false
    }

    private fun loadPreviousChannel(){
        var channel: ChannelItem?
        val currentChannelIndex = if (channelViewModel.currentCategoryId.value == -1L) {
            channelViewModel.currentChannel.value?.indexFavourite!!
        } else {
            channelViewModel.currentChannel.value?.indexGroup!!
        }
        if (::jobSwitchChannel.isInitialized && jobSwitchChannel.isActive) jobSwitchChannel.cancel()
        jobSwitchChannel = lifecycleScope.launch {
            channelViewModel.updateIsLoadingChannel(true)
            channel = channelViewModel.getPreviousChannel(channelViewModel.currentCategoryId.value!!, currentChannelIndex - 1)
            channelViewModel.updateIsLoadingChannel(false)
            loadChannel(channel!!)
        }

    }

    private fun loadNextChannel(){
        println("loadNextChannel")
        var channel: ChannelItem?
        val currentChannelIndex = if (channelViewModel.currentCategoryId.value == -1L) {
            channelViewModel.currentChannel.value?.indexFavourite!!
        } else if (channelViewModel.currentChannel.value != null) {
            channelViewModel.currentChannel.value?.indexGroup!!
        } else {
            0
        }
        if (::jobSwitchChannel.isInitialized && jobSwitchChannel.isActive) jobSwitchChannel.cancel()
        jobSwitchChannel = lifecycleScope.launch {
            channelViewModel.updateIsLoadingChannel(true)
            channel = channelViewModel.getNextChannel(channelViewModel.currentCategoryId.value!!, currentChannelIndex + 1)
            channelViewModel.updateIsLoadingChannel(false)
            loadChannel(channel!!)
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
        isActivityStopped = false
        player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
        isActivityPaused = true
        if (activity?.isInPictureInPictureMode == true) {
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
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        isActivityPaused = false
        if (playerViewModel.currentStreamSource.value != null && !activity?.isInPictureInPictureMode!!) {
            loadStreamSource(playerViewModel.currentStreamSource.value!!)
        }
        player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop")
        isActivityStopped = true
        ProxySelector.setDefault(originalProxySelector)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        ProxySelector.setDefault(originalProxySelector)
        player.release()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPiPModeIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            requireActivity().enterPictureInPictureMode(pipParams)
        }
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
        private const val MAX_DIGITS = 5
        private const val TIMEOUT_UI_CHANNEL_LOAD = 5000L
        private const val TIMEOUT_UI_INFO = 5000L
        private const val RETRY_DELAY_MS = 150L
        private const val BUFFERING_TIMEOUT_MS = 3000L
        private const val SOURCE_LOADING_TIMEOUT_MS = 9000L
        private const val CHANNEL_LOADING_TIMEOUT_MS = 9000L
        private const val HIDE_PLAYER_TIMEOUT_MS = 3000L
        private const val TRIES_EACH_SOURCE = 2
        private const val PLAYING_TIMEOUT_MS = 3000L
        private const val TIME_CACHED_URL_MINUTES = 2L
        private const val DEFAULT_REFRESH_RATE = 50.0f
        private val TAG = PlayerFragment::class.java.name
    }
}

