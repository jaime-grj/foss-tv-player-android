package com.gaarj.iptvplayer.ui.view

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.Display
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.core.MediaUtils
import com.gaarj.iptvplayer.domain.model.StreamSourceItem
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
import com.gaarj.iptvplayer.domain.model.SubtitlesTrack
import com.gaarj.iptvplayer.data.services.ApiService
import com.gaarj.iptvplayer.databinding.ActivityPlayerBinding
import com.gaarj.iptvplayer.domain.model.AudioTrack
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.ChannelSettings
import com.gaarj.iptvplayer.domain.model.MediaInfo
import com.gaarj.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarj.iptvplayer.domain.model.VideoTrack
import com.gaarj.iptvplayer.ui.adapters.AudioTracksAdapter
import com.gaarj.iptvplayer.ui.adapters.ChannelListAdapter
import com.gaarj.iptvplayer.ui.adapters.ChannelSettingsAdapter
import com.gaarj.iptvplayer.ui.adapters.ChannelSourcesAdapter
import com.gaarj.iptvplayer.ui.adapters.SubtitlesTracksAdapter
import com.gaarj.iptvplayer.ui.adapters.VideoTracksAdapter
import com.gaarj.iptvplayer.ui.viewmodel.ChannelViewModel
import com.gaarj.iptvplayer.ui.viewmodel.PlayerViewModel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.video.VideoSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException


@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding

    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector

    private var mediaInfo: MediaInfo = MediaInfo()

    private var currentNumberInput = StringBuilder()

    private lateinit var jobUITimeout: Job
    private lateinit var jobLoadStreamSource: Job
    private lateinit var jobUIChangeChannel: Job
    private lateinit var jobEPGRender : Job

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var rvChannelList: RecyclerView
    private lateinit var rvChannelSettings: RecyclerView
    private lateinit var rvAudioTracks: RecyclerView
    private lateinit var rvSubtitlesTracks: RecyclerView
    private lateinit var rvChannelSources: RecyclerView
    private lateinit var rvVideoTracks: RecyclerView

    private val playerViewModel: PlayerViewModel by viewModels()
    private val channelViewModel: ChannelViewModel by viewModels()

    private val tryNextStreamSourceRunnable = Runnable {
        Log.d("PlayerActivity", "Player error, retrying")
        val currentChannel = channelViewModel.currentChannel.value ?: return@Runnable
        val currentStreamSource = playerViewModel.currentStreamSource.value ?: return@Runnable
        tryNextStreamSource(currentChannel, currentStreamSource)
    }

    private val loadingRunnable = Runnable {
        if (playerViewModel.isLoading.value == true) {
            Log.d("PlayerActivity", "Loading Timeout")
            val currentChannel = channelViewModel.currentChannel.value ?: return@Runnable
            val currentStreamSource = playerViewModel.currentStreamSource.value ?: return@Runnable
            tryNextStreamSource(currentChannel, currentStreamSource)
            playerViewModel.hidePlayer()
        }
    }

    private val bufferingRunnable = Runnable {
        if (playerViewModel.isBuffering.value == true) {
            Log.d("PlayerActivity", "Buffering Timeout")
            val currentChannel = channelViewModel.currentChannel.value ?: return@Runnable
            val currentStreamSource = playerViewModel.currentStreamSource.value ?: return@Runnable
            tryNextStreamSource(currentChannel, currentStreamSource)
            playerViewModel.hidePlayer()
        }
    }

    private val checkPlayingCorrectlyRunnable = Runnable {
        playerViewModel.updateSourcesTriedCount(1)
        playerViewModel.updateTriesCountForEachSource(1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        switchRefreshRate(50f)
        initUI()
        initPlayer()
        channelViewModel.channels.observe(this) { channels ->
            if (channels.isNotEmpty()) {
                initChannelList()
                loadChannel(channels.first())
            }
        }
        initSettingsMenu()
        initAudioTracksMenu()
        initSubtitlesTracksMenu()
        initSourcesMenu()

        binding.buttonPiP.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPiPMode()
            } else {
                Toast.makeText(this, "Picture-in-Picture mode is not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isAndroidTV(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        playerViewModel.onCreate()
        channelViewModel.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars() or WindowInsets.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
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


        playerViewModel.channelName.observe(this) { channelName ->
            binding.channelName.text = channelName
        }

        playerViewModel.channelNumber.observe(this) { channelNumber ->
            binding.channelNumber.text = channelNumber.toString()
        }

        playerViewModel.timeDate.observe(this) { timeDate ->
            binding.timeDate.text = timeDate
        }

        playerViewModel.isChannelListVisible.observe(this) { isVisible ->
            binding.channelList.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isSettingsMenuVisible.observe(this) { isVisible ->
            binding.rvChannelSettings.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isTrackMenuVisible.observe(this) { isVisible ->
            binding.rvChannelTrackSettings.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.mediaInfo.observe(this) { mediaInfo ->
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
            if (mediaInfo.hasMultiLanguageAudio) {
                binding.channelHasMultiLanguageAudio.visibility = View.VISIBLE
            }
            else{
                binding.channelHasMultiLanguageAudio.visibility = View.GONE
            }

            if (mediaInfo.hasTeletext) {
                binding.channelHasTeletext.visibility = View.VISIBLE
            }
            else{
                binding.channelHasTeletext.visibility = View.GONE
            }
        }

        playerViewModel.isMediaInfoVisible.observe(this) { isVisible ->
            binding.channelMediaInfo.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isChannelNameVisible.observe(this) { isVisible ->
            binding.channelName.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isChannelNumberVisible.observe(this) { isVisible ->
            binding.channelNumber.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isTimeDateVisible.observe(this) { isVisible ->
            binding.timeDate.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isErrorMessageVisible.observe(this) { isVisible ->
            binding.message.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isPlayerVisible.observe(this) { isVisible ->
            binding.playerView.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonUpVisible.observe(this) { isVisible ->
            binding.buttonUp.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonDownVisible.observe(this) { isVisible ->
            binding.buttonDown.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonSettingsVisible.observe(this) { isVisible ->
            binding.buttonSettings.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonChannelListVisible.observe(this) { isVisible ->
            binding.buttonChannelList.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isButtonPiPVisible.observe(this) { isVisible ->
            binding.buttonPiP.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        playerViewModel.isChannelNumberKeyboardVisible.observe(this) { isVisible ->
            binding.channelNumberKeyboard.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        channelViewModel.currentProgram.observe(this) { currentProgram ->
            if (currentProgram != null){
                val currentProgramStartTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentProgram.startTime)
                val currentProgramStopTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentProgram.stopTime)
                binding.tvChannelCurrentProgram.text = currentProgram.title + " (" +  currentProgramStartTimeFormatted + " - " + currentProgramStopTimeFormatted + ")"
                binding.tvChannelCurrentProgram.visibility = View.VISIBLE
                binding.tvChannelCurrentProgramPrev.visibility = View.VISIBLE
            } else{
                binding.tvChannelCurrentProgram.visibility = View.GONE
                binding.tvChannelCurrentProgramPrev.visibility = View.GONE
            }
        }

        channelViewModel.nextProgram.observe(this) { nextProgram ->
            if (nextProgram != null){
                val nextProgramStartTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextProgram.startTime)
                val nextProgramStopTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextProgram.stopTime)
                binding.tvChannelNextProgram.text = nextProgram.title + " (" +  nextProgramStartTimeFormatted + " - " + nextProgramStopTimeFormatted + ")"
                binding.tvChannelNextProgram.visibility = View.VISIBLE
                binding.tvChannelNextProgramPrev.visibility = View.VISIBLE
            }
            else{
                binding.tvChannelNextProgram.visibility = View.GONE
                binding.tvChannelNextProgramPrev.visibility = View.GONE
            }
        }

        playerViewModel.isBottomInfoVisible.observe(this) { isVisible ->
            if (isVisible) {
                if (::jobEPGRender.isInitialized && jobEPGRender.isActive) {
                    jobEPGRender.cancel()
                }
                jobEPGRender = CoroutineScope(Dispatchers.IO).launch {
                    val currentChannel = channelViewModel.currentChannel.value
                    if (currentChannel != null){
                        delay(100)
                        channelViewModel.getCurrentProgramForChannel(currentChannel.id)
                    }

                }
                binding.channelBottomInfo.visibility = View.VISIBLE

            }
            else{
                binding.channelBottomInfo.visibility = View.GONE
            }
        }

        channelViewModel.currentProgram.observe(this) { currentProgram ->
            if (currentProgram != null){
                mediaInfo.hasEPG = true
                playerViewModel.updateMediaInfo(mediaInfo)
            }
            else{
                mediaInfo.hasEPG = false
                playerViewModel.updateMediaInfo(mediaInfo)
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
                playerViewModel.showSettingsMenu()
            }
        }

        binding.buttonChannelList.setOnClickListener {
            if (playerViewModel.isChannelListVisible.value == true) {
                playerViewModel.hideChannelList()
            } else {
                playerViewModel.showChannelList()
            }
        }
    }

    private fun switchRefreshRate(frameRate: Float) {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

        val supportedModes = display.supportedModes
        val mode = supportedModes.find { MediaUtils.areRatesEqual(frameRate, it.refreshRate) }

        if (mode != null) {
            val attributes = window.attributes
            attributes.preferredDisplayModeId = mode.modeId
            window.attributes = attributes
            Log.d("DisplayMode", "Switched to refresh rate: ${mode.refreshRate}")
        } else {
            Log.d("DisplayMode", "No matching refresh rate found for $frameRate fps")
        }
    }

    private fun initSettingsMenu() {
        val settingsList = listOf(ChannelSettings(getString(R.string.audio_track)), ChannelSettings(getString(R.string.subtitle_track)), ChannelSettings(getString(R.string.video_track)), ChannelSettings(getString(R.string.change_source)))
        rvChannelSettings.layoutManager = LinearLayoutManager(this)
        rvChannelSettings.adapter = ChannelSettingsAdapter(settingsList) { selectedSetting ->
            if (playerViewModel.isSettingsMenuVisible.value == true) {
                playerViewModel.hideSettingsMenu()
            }
            loadSetting(selectedSetting)
        }
    }

    private fun initAudioTracksMenu() {
        rvAudioTracks.layoutManager = LinearLayoutManager(this)
        rvAudioTracks.adapter = AudioTracksAdapter(listOf()) { selectedAudioTrack ->
            if (playerViewModel.isSettingsMenuVisible.value == true) {
                playerViewModel.hideSettingsMenu()
            }
            loadAudioTrack(selectedAudioTrack)
        }
    }

    private fun initSubtitlesTracksMenu() {
        rvSubtitlesTracks.layoutManager = LinearLayoutManager(this)
        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(listOf()) { selectedSubtitlesTrack ->
            loadSubtitlesTrack(selectedSubtitlesTrack)
        }
    }

    private fun initSourcesMenu() {
        rvChannelSources.layoutManager = LinearLayoutManager(this)
    }

    private fun loadSetting(settingIndex: Int) {
        when (settingIndex) {
            ChannelSettings.AUDIO_TRACKS -> {
                if (isAndroidTV(this)) {
                    initFocusInAudioTracksMenu()
                }
                loadAudioTracksMenu()
            }
            ChannelSettings.SUBTITLES_TRACKS -> {
                if (isAndroidTV(this)) {
                    initFocusInSubtitlesTracksMenu()
                }
                loadSubtitlesTracksMenu()
            }
            ChannelSettings.VIDEO_TRACKS -> {
                if (isAndroidTV(this)) {
                    initFocusInVideoTracksMenu()
                }
                loadVideoTracksMenu()
            }
            ChannelSettings.SOURCES -> {
                if (isAndroidTV(this)) {
                    initFocusInSourcesMenu()
                }
                loadSourcesMenu()
            }
        }

    }

    private fun loadAudioTracksMenu(){
        val audioTrackList = loadAudioTracks()
        rvAudioTracks.adapter = AudioTracksAdapter(audioTrackList) { selectedAudioTrack ->
            loadAudioTrack(selectedAudioTrack)
        }
        if (audioTrackList.isNotEmpty()) {
            playerViewModel.updateCurrentItemSelectedFromAudioTracksMenu(0)
        }
        playerViewModel.showTrackMenu()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.AUDIO_TRACKS)
    }

    private fun loadSubtitlesTracksMenu() {
        val subtitlesTrackList = loadSubtitlesTracks()
        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(subtitlesTrackList) { selectedSubtitlesTrack ->
            loadSubtitlesTrack(selectedSubtitlesTrack)
        }
        if (subtitlesTrackList.isNotEmpty()) {
            playerViewModel.updateCurrentItemSelectedFromSubtitlesTracksMenu(0)
        }
        playerViewModel.showTrackMenu()
        playerViewModel.updateCurrentLoadedMenuSetting(ChannelSettings.SUBTITLES_TRACKS)
    }

    private fun loadVideoTracksMenu() {
        val videoTracksList = loadVideoTracks()
        rvVideoTracks.adapter = VideoTracksAdapter(videoTracksList) { selectedVideoTrack ->
            loadVideoTrack(selectedVideoTrack)
        }
        if (videoTracksList.isNotEmpty()) {
            playerViewModel.updateCurrentItemSelectedFromVideoTracksMenu(0)
        }
        playerViewModel.showTrackMenu()
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
                isSelected = true
            )
        )
        sourcesList += channelViewModel.currentChannel.value?.streamSources!!.sortedBy { it.index }
        for (i in sourcesList.indices) {
            if (i == 0){
                if (playerViewModel.isSourceForced.value == true) {
                    sourcesList[i].isSelected = false
                }
                else {
                    break
                }
            }
            else {
                sourcesList[i].isSelected = sourcesList[i].id == playerViewModel.currentStreamSource.value?.id
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
        if (sourcesList.isNotEmpty()) {
            playerViewModel.updateCurrentItemSelectedFromChannelSourcesMenu(0)
        }
        playerViewModel.showTrackMenu()
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
                    audioTrackList += listOf(AudioTrack(trackFormat.id.orEmpty(), trackFormat.language.orEmpty(), trackGroup.isTrackSelected(j)))
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
        subtitlesTrackList += listOf(SubtitlesTrack("-1", getString(R.string.off), true))

        for (i in 0 until currentTracks.groups.size) {
            val trackGroup = currentTracks.groups[i]
            if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                for (j in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(j)
                    if (trackGroup.isTrackSelected(j)) {
                        subtitlesTrackList.first().isSelected = false
                    }
                    subtitlesTrackList += listOf(SubtitlesTrack(trackFormat.id.orEmpty(), trackFormat.language.orEmpty(), trackGroup.isTrackSelected(j)))
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
        videoTrackList += listOf(VideoTrack("-1", getString(R.string.auto), -1, -1, 0, true))

        for (i in 0 until currentTracks.groups.size) {
            val trackGroup = currentTracks.groups[i]
            if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                for (j in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(j)
                    videoTrackList += listOf(VideoTrack(trackFormat.id.orEmpty(), trackFormat.codecs.orEmpty(), trackFormat.width, trackFormat.height, trackFormat.bitrate / 1000, trackGroup.isTrackSelected(j)))
                    globalVideoTrackIndex++
                }
            }
        }
        return videoTrackList
    }

    private fun initChannelList() {
        rvChannelList.layoutManager = LinearLayoutManager(this)
        val sortedChannels = channelViewModel.getSortedChannelsByIndexFavourite()

        rvChannelList.adapter = ChannelListAdapter(sortedChannels) { selectedChannel ->
            loadChannel(selectedChannel)
        }
    }

    private fun initFocusInChannelList() {
        rvChannelList.viewTreeObserver.addOnGlobalLayoutListener {
            rvChannelList.scrollToPosition(playerViewModel.currentItemSelectedFromChannelList.value ?: 0)
            rvChannelList.post {
                val viewHolder = rvChannelList.findViewHolderForAdapterPosition(playerViewModel.currentItemSelectedFromChannelList.value ?: 0)
                viewHolder?.itemView?.requestFocus()
            }
        }
    }

    private fun initFocusInChannelSettingsMenu() {
        if (isAndroidTV(this)) {
            rvChannelSettings.viewTreeObserver.addOnGlobalLayoutListener {
                rvChannelSettings.scrollToPosition(playerViewModel.currentItemSelectedFromChannelSettingsMenu.value ?: 0)
                rvChannelSettings.post {
                    val viewHolder = rvChannelSettings.findViewHolderForAdapterPosition(playerViewModel.currentItemSelectedFromChannelSettingsMenu.value ?: 0)
                    viewHolder?.itemView?.requestFocus()
                }
            }
        }
    }

    private fun initFocusInAudioTracksMenu() {
        if (isAndroidTV(this)) {
            rvAudioTracks.viewTreeObserver.addOnGlobalLayoutListener {
                rvAudioTracks.scrollToPosition(playerViewModel.currentItemSelectedFromAudioTracksMenu.value ?: 0)
                rvAudioTracks.post {
                    val viewHolder = rvAudioTracks.findViewHolderForAdapterPosition(playerViewModel.currentItemSelectedFromAudioTracksMenu.value ?: 0)
                    viewHolder?.itemView?.requestFocus()
                }
            }
        }
    }

    private fun initFocusInSubtitlesTracksMenu() {
        rvSubtitlesTracks.viewTreeObserver.addOnGlobalLayoutListener {
            rvSubtitlesTracks.scrollToPosition(playerViewModel.currentItemSelectedFromSubtitlesTracksMenu.value ?: 0)
            rvSubtitlesTracks.post {
                val viewHolder = rvSubtitlesTracks.findViewHolderForAdapterPosition(playerViewModel.currentItemSelectedFromSubtitlesTracksMenu.value ?: 0)
                viewHolder?.itemView?.requestFocus()
            }
        }
    }

    private fun initFocusInSourcesMenu() {
        rvChannelSources.viewTreeObserver.addOnGlobalLayoutListener {
            rvChannelSources.scrollToPosition(playerViewModel.currentItemSelectedFromChannelSourcesMenu.value ?: 0)
            rvChannelSources.post {
                val viewHolder = rvChannelSources.findViewHolderForAdapterPosition(playerViewModel.currentItemSelectedFromChannelSourcesMenu.value ?: 0)
                viewHolder?.itemView?.requestFocus()
            }
        }
    }

    private fun initFocusInVideoTracksMenu() {
        rvVideoTracks.viewTreeObserver.addOnGlobalLayoutListener {
            rvVideoTracks.scrollToPosition(playerViewModel.currentItemSelectedFromVideoTracksMenu.value ?: 0)
            rvVideoTracks.post {
                val viewHolder = rvVideoTracks.findViewHolderForAdapterPosition(playerViewModel.currentItemSelectedFromVideoTracksMenu.value ?: 0)
                viewHolder?.itemView?.requestFocus()
            }
        }
    }

    private fun initPlayer(){
        binding.playerView.useController = false

        trackSelector = DefaultTrackSelector(this)
        val parameters = trackSelector
            .buildUponParameters()
            //.setForceHighestSupportedBitrate(true) // Choose the highest quality
            //.setAllowVideoMixedMimeTypeAdaptiveness(false) // Disable adaptive switching between codecs
            //.setMaxVideoBitrate(Int.MAX_VALUE)
            //.setExceedRendererCapabilitiesIfNecessary(true)
            .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        trackSelector.parameters = parameters

        val renderersFactory = DefaultRenderersFactory( this)
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                500,  // Min buffer before starting playback
                15000,  // Max buffer size
                500,   // Min buffer for playback start
                500    // Min buffer for playback resume after a pause
            ).build()

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector).setRenderersFactory(
                renderersFactory
                    .setEnableDecoderFallback(true)
                    .setMediaCodecSelector(MediaCodecSelector.DEFAULT))
            .setLoadControl(loadControl)
            .build()

        setSubtitleTheme()

        binding.playerView.player = player

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                playerViewModel.hidePlayer()
                if (error is ExoPlaybackException) {
                    when (error.type) {
                        ExoPlaybackException.TYPE_SOURCE -> {
                            Log.e("PlayerError", "Source error: ${error.sourceException.message}")
                            handler.postDelayed(tryNextStreamSourceRunnable, RETRY_DELAY_MS)
                        }
                        ExoPlaybackException.TYPE_RENDERER -> {
                            Log.e("PlayerError", "Renderer error: ${error.rendererException.message}")
                            handler.postDelayed(tryNextStreamSourceRunnable, RETRY_DELAY_MS)
                        }
                        ExoPlaybackException.TYPE_UNEXPECTED -> {
                            Log.e("PlayerError", "Unexpected error: ${error.unexpectedException.message}")
                            handler.postDelayed(tryNextStreamSourceRunnable, RETRY_DELAY_MS)
                        }
                        // Handle other types of errors as needed
                        ExoPlaybackException.TYPE_REMOTE -> {

                        }
                    }
                }
                handler.postDelayed(tryNextStreamSourceRunnable, RETRY_DELAY_MS)
            }

            override fun onRenderedFirstFrame() {
                if (playerViewModel.isErrorMessageVisible.value == true) playerViewModel.hideErrorMessage()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (playerViewModel.isLoading.value == true) cancelLoadingTimer()
                    if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
                    cancelTryNextStreamSourceTimer()
                    playerViewModel.hideErrorMessage()
                    playerViewModel.showPlayer()
                }
                else if (playbackState == Player.STATE_ENDED){
                    if (playerViewModel.isLoading.value == true) cancelLoadingTimer()
                    if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
                    cancelCheckPlayingCorrectlyTimer()
                    if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                    playerViewModel.hidePlayer()
                    if (playerViewModel.isLoading.value == false && playerViewModel.isBuffering.value == false) handler.postDelayed(tryNextStreamSourceRunnable, RETRY_DELAY_MS)
                }
                else if (playbackState == Player.STATE_BUFFERING){
                    Log.d("PlayerActivity", "buffering")

                    cancelCheckPlayingCorrectlyTimer()
                    if (playerViewModel.isLoading.value == false) startBufferingTimer()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (playerViewModel.isLoading.value == true) cancelLoadingTimer()
                    if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
                    cancelTryNextStreamSourceTimer()

                    if (playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.AUDIO_TRACKS && rvAudioTracks.adapter?.itemCount == 0) {
                        val audioTrackList = loadAudioTracks()

                        rvAudioTracks.adapter = AudioTracksAdapter(audioTrackList) { selectedAudioTrack ->
                            loadAudioTrack(selectedAudioTrack)
                        }
                        playerViewModel.updateCurrentItemSelectedFromAudioTracksMenu(0)
                    }
                    else if (playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.SUBTITLES_TRACKS && rvSubtitlesTracks.adapter?.itemCount == 1) {
                        val subtitlesTrackList = loadSubtitlesTracks()
                        rvSubtitlesTracks.adapter = SubtitlesTracksAdapter(subtitlesTrackList) { selectedSubtitlesTrack ->
                            loadSubtitlesTrack(selectedSubtitlesTrack)
                        }
                        playerViewModel.updateCurrentItemSelectedFromSubtitlesTracksMenu(0)
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

                    if (binding.channelName.visibility == View.VISIBLE && playerViewModel.isLoading.value == false) {
                        playerViewModel.showMediaInfo()
                    }
                    startCheckPlayingCorrectlyTimer()
                }
                binding.playerView.keepScreenOn = isPlaying
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioLanguages = mutableListOf<String>()
                for (i in 0 until tracks.groups.size) {
                    val trackGroup = tracks.groups[i]
                    if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                        for (j in 0 until trackGroup.length) {
                            val trackFormat = trackGroup.getTrackFormat(j)
                            Log.d("PlayerActivity", trackFormat.toString())
                            val audioLanguage = trackFormat.language ?: "Unknown"
                            if(audioLanguage !in audioLanguages){
                                audioLanguages += listOf(audioLanguage)
                                Log.d("PlayerActivity", audioLanguages.toString())
                            }
                            if (trackGroup.isTrackSelected(j)){
                                val audioCodec: String?
                                audioCodec = if (trackFormat.codecs != null) {
                                    trackFormat.codecs
                                } else {
                                    trackFormat.sampleMimeType
                                }
                                MediaUtils.getUserFriendlyCodec(audioCodec)?.let { mediaInfo.audioCodec = it }
                                if (trackFormat.bitrate > 0) mediaInfo.audioBitrate = (trackFormat.bitrate / 1000).toString() + " kbps" else mediaInfo.audioBitrate = null
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
                                if (trackFormat.sampleRate > 0){
                                    mediaInfo.audioSamplingRate = DecimalFormat("0.#").format(trackFormat.sampleRate.toFloat() / 1000).toString() + " kHz"
                                }
                                else {
                                    mediaInfo.audioSamplingRate = null
                                }
                            }
                            if (audioLanguages.size > 1) {
                                mediaInfo.hasMultiLanguageAudio = true
                            }
                            else{
                                mediaInfo.hasMultiLanguageAudio = false
                            }
                        }
                    }
                    if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                        mediaInfo.hasSubtitles = true
                    }
                    /*if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                        for (j in 0 until trackGroup.length) {
                            val trackFormat = trackGroup.getTrackFormat(j)
                        }
                    }*/
                    val currentProgram = channelViewModel.currentProgram.value
                    if (currentProgram != null){
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
                mediaInfo.videoResolution = "${videoSize.width}x${videoSize.height}"
                mediaInfo.videoQuality = MediaUtils.calculateVideoQuality(videoSize.width, videoSize.height)
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
                if (format.bitrate > 0) mediaInfo.audioBitrate =(format.bitrate / 1000).toString() + " kbps" else mediaInfo.audioBitrate = null
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
                    mediaInfo.videoFrameRate = (format.frameRate.toInt().toString() + " FPS")
                } else mediaInfo.videoFrameRate = null
                val frameRateSwitch: Float = if (MediaUtils.areRatesEqual(format.frameRate, 25.0f)){
                    50.0f
                } else if (MediaUtils.areRatesEqual(format.frameRate, 30.0f)){
                    60.0f
                } else if (format.frameRate > 0.0f){
                    format.frameRate
                } else{
                    50.0f
                }
                if (playerViewModel.currentStreamSource.value?.refreshRate == 0f) {
                    switchRefreshRate(frameRateSwitch)
                }
                mediaInfo.videoAspectRatio = MediaUtils.getHumanReadableAspectRatio(format.width.toFloat() / format.height * format.pixelWidthHeightRatio)
                if (format.bitrate > 0){
                    mediaInfo.videoBitrate = DecimalFormat("0.##").format((format.bitrate.toFloat() / 1_000_000.0)) + " Mbps"
                }
                else mediaInfo.videoBitrate = null
                playerViewModel.updateMediaInfo(mediaInfo)
            }
        })
    }

    private fun setSubtitleTheme() {
        val subtitleView: SubtitleView? = binding.playerView.subtitleView

        val foregroundColor = Color.WHITE  // Text color
        val backgroundColor = Color.TRANSPARENT  // Background color
        val windowColor = Color.TRANSPARENT  // Window background

        val customTypeface: Typeface? = ResourcesCompat.getFont(this, R.font.lato_bold)

        val captionStyle = CaptionStyleCompat(
            foregroundColor,
            backgroundColor,
            windowColor,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            Color.BLACK,
            customTypeface
        )
        subtitleView?.setStyle(captionStyle)

        subtitleView?.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.05f)

        val params = subtitleView?.layoutParams as? FrameLayout.LayoutParams
        params?.setMargins(25, 25, 25, 25)
        subtitleView?.layoutParams = params
    }

    private fun tryNextStreamSource(channel: ChannelItem, currentStreamSource: StreamSourceItem) {
        Log.d("PlayerActivity", "tryNextStreamSource")
        if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
        if (player.isPlaying || player.isLoading) player.stop()
        if (playerViewModel.isSourceForced.value == true){
            loadStreamSource(currentStreamSource)
            playerViewModel.updateTriesCountForEachSource(playerViewModel.triesCountForEachSource.value!! + 1)
            if (playerViewModel.triesCountForEachSource.value!! > TRIES_EACH_SOURCE){
                playerViewModel.showErrorMessage()
            }
        }
        else{
            Log.d("PlayerActivity", "triesforeachsource: $playerViewModel.triesCountForEachSource.value")

            val streamSourcesFiltered = channel.streamSources.filter { it.index > currentStreamSource.index }
            var newStreamSourceIndex = streamSourcesFiltered.minByOrNull { it.index }?.index ?: channel.streamSources.minByOrNull { it.index }?.index ?: 0

            val streamSourceMaxIndex = channel.streamSources.maxByOrNull{ it.index }?.index ?: 0
            val streamSourcesCount = channel.streamSources.size
            if (playerViewModel.triesCountForEachSource.value!! == TRIES_EACH_SOURCE){

                if (newStreamSourceIndex > streamSourceMaxIndex) {
                    newStreamSourceIndex = channel.streamSources.minBy { it.index }.index
                }
                val newStreamSource: StreamSourceItem? = channel.streamSources.firstOrNull { it.index == newStreamSourceIndex }
                Log.d("PlayerActivity", "newStreamSource:$newStreamSource")
                if (newStreamSource != null) {
                    loadStreamSource(newStreamSource)
                    playerViewModel.updateSourcesTriedCount(playerViewModel.sourcesTriedCount.value!! + 1)
                    Log.d("TryNextStreamSource", "sourcesTriedCount: $playerViewModel.sourcesTriedCount.value")
                    playerViewModel.updateTriesCountForEachSource(1)
                }
                Log.d("PlayerActivity", "streamsourcemaxindex $streamSourceMaxIndex")
                Log.d("PlayerActivity", "sourcesTriedCount: $playerViewModel.sourcesTriedCount.value")
                Log.d("PlayerActivity", "newStreamSourceIndex: $newStreamSourceIndex")
                if (playerViewModel.sourcesTriedCount.value!! > streamSourcesCount) {
                    playerViewModel.showErrorMessage()
                }
            }
            else{
                Log.d("PlayerActivity", "Load same source: $currentStreamSource")
                loadStreamSource(currentStreamSource)
                playerViewModel.updateTriesCountForEachSource(playerViewModel.triesCountForEachSource.value!! + 1)
            }
        }
    }

    private fun startLoadingTimer() {
        Log.d("PlayerActivity", "startLoadingTimer")

        playerViewModel.setIsLoading(true)
        handler.postDelayed(loadingRunnable, LOADING_TIMEOUT_MS)
    }

    private fun startBufferingTimer() {
        Log.d("PlayerActivity", "startBufferingTimer")
        playerViewModel.setIsBuffering(true)
        handler.postDelayed(bufferingRunnable, BUFFERING_TIMEOUT_MS)
    }

    private fun startCheckPlayingCorrectlyTimer() {
        Log.d("PlayerActivity", "startCheckPlayingCorrectlyTimer")

        handler.postDelayed(checkPlayingCorrectlyRunnable, PLAYING_TIMEOUT_MS)
    }

    private fun cancelCheckPlayingCorrectlyTimer() {
        Log.d("PlayerActivity", "cancelCheckPlayingCorrectlyTimer")
        handler.removeCallbacks(checkPlayingCorrectlyRunnable)
    }

    private fun cancelBufferingTimer() {
        Log.d("PlayerActivity", "cancelBufferingTimer")
        playerViewModel.setIsBuffering(false)
        handler.removeCallbacks(bufferingRunnable)
    }

    private fun cancelLoadingTimer() {
        Log.d("PlayerActivity", "cancelLoadingTimer")

        playerViewModel.setIsLoading(false)
        handler.removeCallbacks(loadingRunnable)
    }

    private fun cancelTryNextStreamSourceTimer() {
        Log.d("PlayerActivity", "cancelTryNextStreamSourceTimer")
        handler.removeCallbacks(tryNextStreamSourceRunnable)
    }

    private fun loadChannel(channel: ChannelItem) {
        if (::jobLoadStreamSource.isInitialized && (jobLoadStreamSource.isActive)) {
            jobLoadStreamSource.cancel()
        }
        currentNumberInput.clear()
        if (channel.id < 0 || !channelViewModel.channelExists(channel.id)) {
            if (binding.channelNumber.visibility == View.VISIBLE) playerViewModel.hideChannelNumber()
            if (binding.channelName.visibility == View.VISIBLE) playerViewModel.hideChannelName()
            return
        }
        if (playerViewModel.isLoading.value == true) cancelLoadingTimer()
        if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
        cancelCheckPlayingCorrectlyTimer()
        cancelTryNextStreamSourceTimer()

        if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
        resetMediaInfo()

        if (player.isPlaying || player.isLoading){
            player.stop()
        }
        playerViewModel.hidePlayer()

        if (binding.message.visibility == View.VISIBLE) playerViewModel.hideErrorMessage()
        Log.d("PlayerActivity",channel.name)

        val sortedChannels = channelViewModel.getSortedChannelsByIndexFavourite()

        for (i in sortedChannels.indices) {
            if (sortedChannels[i] == channel) {
                playerViewModel.updateCurrentItemSelectedFromChannelList(i)
                break
            }
        }

        playerViewModel.updateChannelName(channel.name)
        channel.indexFavourite?.let { playerViewModel.updateChannelNumber(it) }

        channelViewModel.updateCurrentProgram(null)
        channelViewModel.updateNextProgram(null)



        showChannelInfoWithTimeout(timeout = TIMEOUT_UI_CHANNEL_LOAD)
        if (channelViewModel.currentChannel.value == channel) {
            return
        }
        val streamSources = channel.streamSources

        playerViewModel.updateIsSourceForced(false)
        //if (nextProgram != null) playerViewModel.updateNextProgram(nextProgram!!["title"]!!)
        channelViewModel.updateCurrentChannel(channel)
        if (streamSources.isNotEmpty()) {
            loadStreamSource(streamSources.minBy { it.index })
            playerViewModel.updateTriesCountForEachSource(1)
            playerViewModel.updateSourcesTriedCount(1)
        }
    }

    private fun loadStreamSource(streamSource: StreamSourceItem) {
        if (::jobLoadStreamSource.isInitialized && (jobLoadStreamSource.isActive)) {
            jobLoadStreamSource.cancel()
        }
        if (playerViewModel.isLoading.value == true) cancelLoadingTimer()
        if (playerViewModel.isBuffering.value == true) cancelBufferingTimer()
        cancelCheckPlayingCorrectlyTimer()
        cancelTryNextStreamSourceTimer()

        if (player.isLoading || player.isPlaying) {
            player.stop()
        }

        startLoadingTimer()
        resetMediaInfo()
        if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
        playerViewModel.updateCurrentStreamSource(streamSource)
        jobLoadStreamSource = CoroutineScope(Dispatchers.IO).launch {
            delay(250)
            val url: String = if (!streamSource.apiCalls.isNullOrEmpty()) {
                ApiService.getURLFromChannelSource(streamSource)!!
            } else{
                streamSource.url
            }
            ensureActive()

            val headersObj: List<StreamSourceHeaderItem> = streamSource.headers ?: emptyList()
            val headers = ApiService.getHeadersMapFromHeadersObject(headersObj)

            Log.d("PlayerActivity","Stream URL: $url")
            val httpDataSourceFactory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers)
            val mediaSource = if (url.contains(".m3u8")) {
                HlsMediaSource.Factory(httpDataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(url)))
            }
            /*else if (url.contains(".mpd")) {
                DashMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(
                        MediaItem.Builder().setUri(Uri.parse(url))
                            .setDrmConfiguration(
                                MediaItem.DrmConfiguration
                                    .Builder(C.WIDEVINE_UUID)
                                    .setLicenseUri("https://lic.drmtoday.com/license-proxy-widevine/cenc/")
                                    .setLicenseRequestHeaders(
                                        mapOf("Host" to "lic.drmtoday.com",
                                            "Origin" to "https://www.rtp.pt",
                                            "Referer" to "https://www.rtp.pt/",
                                            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0",
                                            "x-dt-custom-data" to "eyJ1c2VySWQiOiJwdXJjaGFzZSIsInNlc3Npb25JZCI6InAwIiwibWVyY2hhbnQiOiJtb2dfcnRwIn0=",
                                            "Accept-Encoding" to "gzip, deflate, br, zstd")
                                    )
                                    .build()
                            )
                            .setMimeType(MimeTypes.APPLICATION_MPD)
                            .build()
                    )
            }*/
            else {
                ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(url)))
            }

            ensureActive()

            runOnUiThread{
                player.setMediaSource(mediaSource)

                if (streamSource.refreshRate != null) switchRefreshRate(streamSource.refreshRate) else switchRefreshRate(50f)

                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverrides()
                    .build()
                player.prepare()
                player.play()
                playerViewModel.showPlayer()
            }
        }
    }

    private fun loadSubtitlesTrack(subtitlesTrack: SubtitlesTrack){
        if (subtitlesTrack.id == "-1") {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverrides()
                .build()
            return
        }
        val tracks = player.currentTracks
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val trackFormat = trackGroup.getTrackFormat(i)
                    if (subtitlesTrack.language == trackFormat.language && subtitlesTrack.id == trackFormat.id) {
                        player.trackSelectionParameters =
                            player.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                                .build()
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
                    if (audioTrack.language == trackFormat.language && audioTrack.id == trackFormat.id) {
                        player.trackSelectionParameters =
                            player.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                                .build()
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
        mediaInfo.hasMultiLanguageAudio = false
        playerViewModel.updateMediaInfo(mediaInfo)
    }

    /* This will need to be refactored */
    private fun showChannelInfoWithTimeout(timeout: Long){
        if (::jobUITimeout.isInitialized && jobUITimeout.isActive) {
            jobUITimeout.cancel()
        }

        jobUITimeout = CoroutineScope(Dispatchers.IO).launch {
            try{
                runOnUiThread {
                    if (!isAndroidTV(this@PlayerActivity)) {
                        playerViewModel.showButtonUp()
                        playerViewModel.showButtonDown()
                        playerViewModel.showButtonChannelList()
                        playerViewModel.showButtonSettings()
                        playerViewModel.showButtonPiP()
                    }
                    playerViewModel.hideTimeDate()
                    playerViewModel.showChannelNumber()
                    playerViewModel.showChannelName()
                    playerViewModel.showBottomInfo()
                    if (playerViewModel.isLoading.value == false) {
                        playerViewModel.showMediaInfo()
                    }
                }
                delay(timeout)
                ensureActive()
                runOnUiThread {
                    if (!isAndroidTV(this@PlayerActivity)) {
                        playerViewModel.hideButtonUp()
                        playerViewModel.hideButtonDown()
                        playerViewModel.hideButtonChannelList()
                        playerViewModel.hideButtonSettings()
                        playerViewModel.hideButtonPiP()
                    }
                    playerViewModel.hideChannelNumber()
                    playerViewModel.hideChannelName()
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
        jobUITimeout = CoroutineScope(Dispatchers.IO).launch {
            try{
                runOnUiThread {

                    if (!isAndroidTV(this@PlayerActivity)) {
                        playerViewModel.showButtonUp()
                        playerViewModel.showButtonDown()
                        playerViewModel.showButtonChannelList()
                        playerViewModel.showButtonSettings()
                        playerViewModel.showButtonPiP()
                    }
                    playerViewModel.showChannelNumber()
                    playerViewModel.showChannelName()
                    playerViewModel.updateTimeDate()
                    playerViewModel.showTimeDate()
                    if (playerViewModel.isLoading.value == false) {
                        playerViewModel.showMediaInfo()
                    }
                    playerViewModel.showBottomInfo()
                }
                delay(timeout)
                ensureActive()
                runOnUiThread {
                    if (!isAndroidTV(this@PlayerActivity)) {
                        playerViewModel.hideButtonUp()
                        playerViewModel.hideButtonDown()
                        playerViewModel.hideButtonChannelList()
                        playerViewModel.hideButtonSettings()
                        playerViewModel.hideButtonPiP()
                    }
                    playerViewModel.hideChannelNumber()
                    playerViewModel.hideChannelName()
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
        CoroutineScope(Dispatchers.IO).launch {
            try{
                runOnUiThread {
                    playerViewModel.showChannelNumberKeyboard()
                    playerViewModel.hideBottomInfo()
                }
                delay(timeout)
                ensureActive()
                val newChannel = channelViewModel.getChannelByFavouriteId(currentNumberInput.toString().toInt())
                runOnUiThread {
                    playerViewModel.hideChannelNumberKeyboard()
                    if (newChannel != null && newChannel != channelViewModel.currentChannel.value) {
                        loadChannel(newChannel)
                    }
                    else if (newChannel == channelViewModel.currentChannel.value){
                        showChannelInfoWithTimeout(TIMEOUT_UI_CHANNEL_LOAD)
                    }
                }
                currentNumberInput.clear()
            } catch (_: CancellationException){
                runOnUiThread {
                    playerViewModel.hideChannelNumber()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val action = event.action
        val isDown = action == 0

        if (!isDown) return super.dispatchKeyEvent(event)
        else{
            when (code) {
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    // Load channel from channel list
                    if (playerViewModel.isChannelListVisible.value == true) {
                        var currentChannelIndex: Int = -1
                        val sortedChannels = channelViewModel.getSortedChannelsByIndexFavourite()
                        for (i in sortedChannels.indices){
                            if (sortedChannels[i] == channelViewModel.currentChannel.value){
                                currentChannelIndex = i
                            }
                        }
                        val currentItemSelectedFromChannelList = playerViewModel.currentItemSelectedFromChannelList.value ?: 0
                        if (currentItemSelectedFromChannelList != currentChannelIndex){
                            val newChannel = (rvChannelList.adapter as? ChannelListAdapter)?.getItemAtPosition(currentItemSelectedFromChannelList) as ChannelItem
                            loadChannel(newChannel)
                        }
                    }
                    // Enter specific setting
                    else if (playerViewModel.isSettingsMenuVisible.value == true) {
                        playerViewModel.hideSettingsMenu()
                        val currentItemSelectedFromChannelSettingsMenu = playerViewModel.currentItemSelectedFromChannelSettingsMenu.value ?: 0
                        when (currentItemSelectedFromChannelSettingsMenu) {
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
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.AUDIO_TRACKS) {
                        val currentItemSelectedFromAudioTracksMenu = playerViewModel.currentItemSelectedFromAudioTracksMenu.value ?: 0
                        if (currentItemSelectedFromAudioTracksMenu >= 0){
                            val audioTrack = (rvAudioTracks.adapter as? AudioTracksAdapter)?.getItemAtPosition(currentItemSelectedFromAudioTracksMenu) as AudioTrack
                            loadAudioTrack(audioTrack)
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.SUBTITLES_TRACKS) {
                        val currentItemSelectedFromSubtitlesTracksMenu = playerViewModel.currentItemSelectedFromSubtitlesTracksMenu.value ?: 0
                        if (currentItemSelectedFromSubtitlesTracksMenu >= 0){
                            val subtitlesTrack = (rvSubtitlesTracks.adapter as? SubtitlesTracksAdapter)?.getItemAtPosition(currentItemSelectedFromSubtitlesTracksMenu) as SubtitlesTrack
                            loadSubtitlesTrack(subtitlesTrack)
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.SOURCES) {
                        val currentItemSelectedFromSourcesMenu = playerViewModel.currentItemSelectedFromChannelSourcesMenu.value ?: 0
                        if (currentItemSelectedFromSourcesMenu >= 0){
                            if (playerViewModel.isErrorMessageVisible.value == true) {
                                playerViewModel.hideErrorMessage()
                            }
                            var streamSource = (rvChannelSources.adapter as? ChannelSourcesAdapter)?.getItemAtPosition(currentItemSelectedFromSourcesMenu) as StreamSourceItem
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
                            loadStreamSource(streamSource)
                            playerViewModel.hideTrackMenu()
                        }
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true && playerViewModel.currentLoadedMenuSetting.value == ChannelSettings.VIDEO_TRACKS) {
                        val currentItemSelectedFromVideoTracksMenu = playerViewModel.currentItemSelectedFromVideoTracksMenu.value ?: 0
                        if (currentItemSelectedFromVideoTracksMenu >= 0){
                            val videoTrack = (rvVideoTracks.adapter as? VideoTracksAdapter)?.getItemAtPosition(currentItemSelectedFromVideoTracksMenu) as VideoTrack
                            loadVideoTrack(videoTrack)
                        }
                    }
                    // Show current channel info
                    else{
                        showFullChannelUIWithTimeout(timeout = TIMEOUT_UI_INFO)
                    }

                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (binding.channelList.visibility == View.VISIBLE) { // Navigate through menu
                        var currentItemSelectedFromChannelList = playerViewModel.currentItemSelectedFromChannelList.value ?: 0
                        val channelsSize = channelViewModel.channels.value?.size ?: 0
                        currentItemSelectedFromChannelList = if (currentItemSelectedFromChannelList - 1 < 0) {
                            channelsSize - 1
                        } else {
                            (currentItemSelectedFromChannelList - 1) % channelsSize
                        }
                        playerViewModel.updateCurrentItemSelectedFromChannelList(currentItemSelectedFromChannelList)
                    }
                    else if (binding.rvChannelSettings.visibility == View.VISIBLE) {
                        var currentItemSelectedFromChannelSettingsMenu = playerViewModel.currentItemSelectedFromChannelSettingsMenu.value ?: 0
                        currentItemSelectedFromChannelSettingsMenu = if (currentItemSelectedFromChannelSettingsMenu - 1 < 0) {
                            rvChannelSettings.adapter!!.itemCount - 1
                        } else {
                            (currentItemSelectedFromChannelSettingsMenu - 1) % rvChannelSettings.adapter!!.itemCount
                        }
                        playerViewModel.updateCurrentItemSelectedFromChannelSettingsMenu(currentItemSelectedFromChannelSettingsMenu)
                    }
                    else if (binding.rvChannelTrackSettings.visibility == View.VISIBLE) {
                        if (rvAudioTracks.adapter!!.itemCount == 0 || rvSubtitlesTracks.adapter!!.itemCount == 0) return true
                        val currentLoadedMenuSetting = playerViewModel.currentLoadedMenuSetting.value ?: 0
                        when (currentLoadedMenuSetting) {
                            ChannelSettings.AUDIO_TRACKS -> {
                                var currentItemSelectedFromAudioTracksMenu = playerViewModel.currentItemSelectedFromAudioTracksMenu.value ?: 0
                                currentItemSelectedFromAudioTracksMenu = if (currentItemSelectedFromAudioTracksMenu - 1 < 0) {
                                    rvAudioTracks.adapter!!.itemCount - 1
                                } else {
                                    (currentItemSelectedFromAudioTracksMenu - 1) % rvAudioTracks.adapter!!.itemCount
                                }
                                playerViewModel.updateCurrentItemSelectedFromAudioTracksMenu(currentItemSelectedFromAudioTracksMenu)
                            }
                            ChannelSettings.SUBTITLES_TRACKS -> {
                                var currentItemSelectedFromSubtitlesTracksMenu = playerViewModel.currentItemSelectedFromSubtitlesTracksMenu.value ?: 0
                                currentItemSelectedFromSubtitlesTracksMenu = if (currentItemSelectedFromSubtitlesTracksMenu - 1 < 0) {
                                    rvSubtitlesTracks.adapter!!.itemCount - 1
                                } else {
                                    (currentItemSelectedFromSubtitlesTracksMenu - 1) % rvSubtitlesTracks.adapter!!.itemCount
                                }
                                playerViewModel.updateCurrentItemSelectedFromSubtitlesTracksMenu(currentItemSelectedFromSubtitlesTracksMenu)
                            }
                            ChannelSettings.SOURCES -> {
                                var currentItemSelectedFromSourcesMenu = playerViewModel.currentItemSelectedFromChannelSourcesMenu.value ?: 0
                                currentItemSelectedFromSourcesMenu = if (currentItemSelectedFromSourcesMenu - 1 < 0) {
                                    rvChannelSources.adapter!!.itemCount - 1
                                } else {
                                    (currentItemSelectedFromSourcesMenu - 1) % rvChannelSources.adapter!!.itemCount
                                }
                                playerViewModel.updateCurrentItemSelectedFromChannelSourcesMenu(currentItemSelectedFromSourcesMenu)
                            }
                            ChannelSettings.VIDEO_TRACKS -> {
                                var currentItemSelectedFromVideoTracksMenu = playerViewModel.currentItemSelectedFromVideoTracksMenu.value ?: 0
                                currentItemSelectedFromVideoTracksMenu = if (currentItemSelectedFromVideoTracksMenu - 1 < 0) {
                                    rvVideoTracks.adapter!!.itemCount - 1
                                } else {
                                    (currentItemSelectedFromVideoTracksMenu - 1) % rvVideoTracks.adapter!!.itemCount
                                }
                                playerViewModel.updateCurrentItemSelectedFromVideoTracksMenu(currentItemSelectedFromVideoTracksMenu)
                            }
                        }
                    }
                    else{
                        loadNextChannel()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (binding.channelList.visibility == View.VISIBLE) { // Navigate through menu
                        var currentItemSelectedFromChannelList = playerViewModel.currentItemSelectedFromChannelList.value ?: 0
                        val channelsSize = channelViewModel.channels.value?.size ?: 0
                        currentItemSelectedFromChannelList = (currentItemSelectedFromChannelList + 1) % channelsSize
                        playerViewModel.updateCurrentItemSelectedFromChannelList(currentItemSelectedFromChannelList)
                    }
                    else if (binding.rvChannelSettings.visibility == View.VISIBLE) { // Navigate through menu
                        var currentItemSelectedFromChannelSettingsMenu = playerViewModel.currentItemSelectedFromChannelSettingsMenu.value ?: 0
                        currentItemSelectedFromChannelSettingsMenu = (currentItemSelectedFromChannelSettingsMenu + 1) % rvChannelSettings.adapter!!.itemCount
                        playerViewModel.updateCurrentItemSelectedFromChannelSettingsMenu(currentItemSelectedFromChannelSettingsMenu)
                    }
                    else if (binding.rvChannelTrackSettings.visibility == View.VISIBLE) {
                        if (rvAudioTracks.adapter!!.itemCount == 0 || rvSubtitlesTracks.adapter!!.itemCount == 0) return true
                        when (playerViewModel.currentLoadedMenuSetting.value) {
                            ChannelSettings.AUDIO_TRACKS -> {
                                var currentItemSelectedFromAudioTracksMenu = playerViewModel.currentItemSelectedFromAudioTracksMenu.value ?: 0
                                currentItemSelectedFromAudioTracksMenu = (currentItemSelectedFromAudioTracksMenu + 1) % rvAudioTracks.adapter!!.itemCount
                                playerViewModel.updateCurrentItemSelectedFromAudioTracksMenu(currentItemSelectedFromAudioTracksMenu)
                            }
                            ChannelSettings.SUBTITLES_TRACKS -> {
                                var currentItemSelectedFromSubtitlesTracksMenu = playerViewModel.currentItemSelectedFromSubtitlesTracksMenu.value ?: 0
                                currentItemSelectedFromSubtitlesTracksMenu = (currentItemSelectedFromSubtitlesTracksMenu + 1) % rvSubtitlesTracks.adapter!!.itemCount
                                playerViewModel.updateCurrentItemSelectedFromSubtitlesTracksMenu(currentItemSelectedFromSubtitlesTracksMenu)
                            }
                            ChannelSettings.SOURCES -> {
                                var currentItemSelectedFromSourcesMenu = playerViewModel.currentItemSelectedFromChannelSourcesMenu.value ?: 0
                                currentItemSelectedFromSourcesMenu = (currentItemSelectedFromSourcesMenu + 1) % rvChannelSources.adapter!!.itemCount
                                playerViewModel.updateCurrentItemSelectedFromChannelSourcesMenu(currentItemSelectedFromSourcesMenu)
                            }
                            ChannelSettings.VIDEO_TRACKS -> {
                                var currentItemSelectedFromVideoTracksMenu = playerViewModel.currentItemSelectedFromVideoTracksMenu.value ?: 0
                                currentItemSelectedFromVideoTracksMenu = (currentItemSelectedFromVideoTracksMenu + 1) % rvVideoTracks.adapter!!.itemCount
                                playerViewModel.updateCurrentItemSelectedFromVideoTracksMenu(currentItemSelectedFromVideoTracksMenu)
                            }
                        }
                    }
                    else{ // Change to previous channel
                        loadPreviousChannel()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (binding.rvChannelSettings.visibility == View.VISIBLE) {
                        playerViewModel.hideSettingsMenu()
                        return true
                    }
                    else if (binding.channelList.visibility == View.VISIBLE) {
                        playerViewModel.hideChannelList()
                    }
                    else if (binding.rvChannelTrackSettings.visibility == View.VISIBLE) {
                        return true
                    }
                    initFocusInChannelSettingsMenu()
                    playerViewModel.showSettingsMenu()
                    return true
                }

                KeyEvent.KEYCODE_MENU -> {
                    if (binding.channelList.visibility == View.VISIBLE) {
                        playerViewModel.hideChannelList()
                        val channels = channelViewModel.channels.value!!
                        for (i in channels.indices){
                            if (channels[i] == channelViewModel.currentChannel.value){
                                playerViewModel.updateCurrentItemSelectedFromChannelList(i)
                                break
                            }
                        }
                        return true
                    }
                    else if (playerViewModel.isSettingsMenuVisible.value == true) {
                        playerViewModel.hideSettingsMenu()
                    }
                    else if (playerViewModel.isTrackMenuVisible.value == true) {
                        playerViewModel.hideTrackMenu()
                    }
                    initFocusInChannelList()
                    playerViewModel.showChannelList()
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
                    else{
                        return super.dispatchKeyEvent(event)
                    }
                }

                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                    if (binding.channelList.visibility == View.VISIBLE) return true
                    if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                    if (playerViewModel.isChannelNameVisible.value == true) playerViewModel.hideChannelName()
                    if (playerViewModel.isChannelNumberVisible.value == true) playerViewModel.hideChannelNumber()
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
        return super.dispatchKeyEvent(event)
    }

    private fun loadPreviousChannel(){
        var channel: ChannelItem? = null
        var currentChannelIndex = channelViewModel.currentChannel.value?.indexFavourite!!
        while (channel == null) {
            currentChannelIndex--
            if (currentChannelIndex < 1) {
                val lastChannelId = channelViewModel.channels.value
                    ?.filter { it.indexFavourite != null }
                    ?.maxByOrNull { it.indexFavourite!! }?.indexFavourite
                currentChannelIndex = lastChannelId!!
            }
            channel = channelViewModel.getChannelByFavouriteId(currentChannelIndex)
        }
        loadChannel(channel)
    }

    private fun loadNextChannel(){
        var channel: ChannelItem? = null
        var currentChannelIndex = channelViewModel.currentChannel.value?.indexFavourite!!
        while (channel == null) {
            currentChannelIndex++
            val lastChannelId = channelViewModel.channels.value
                ?.filter { it.indexFavourite != null }
                ?.maxByOrNull { it.indexFavourite!! }?.indexFavourite
            if (currentChannelIndex - 1 == lastChannelId!!) {
                currentChannelIndex = 1
            }
            channel = channelViewModel.getChannelByFavouriteId(currentChannelIndex)
        }
        loadChannel(channel)
    }

    override fun onStart() {
        super.onStart()
        player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        if (isInPictureInPictureMode) {
            player.playWhenReady = true
            playerViewModel.hideButtonPiP()
            playerViewModel.hideButtonSettings()
            playerViewModel.hideButtonUp()
            playerViewModel.hideButtonDown()
            playerViewModel.hideButtonChannelList()
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

    override fun onStop() {
        super.onStop()
        player.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPiPMode() {
        val aspectRatio = Rational(16, 9) // PiP window aspect ratio
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        enterPictureInPictureMode(params) // Enter PiP mode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isAndroidTV(this)) {
                enterPiPMode()
            }
        }
    }

    companion object {
        private const val MAX_DIGITS = 5
        private const val TIMEOUT_UI_CHANNEL_LOAD = 5000L
        private const val TIMEOUT_UI_INFO = 5000L
        private const val RETRY_DELAY_MS = 500L
        private const val BUFFERING_TIMEOUT_MS = 3000L
        private const val LOADING_TIMEOUT_MS = 6000L
        private const val TRIES_EACH_SOURCE = 2
        private const val PLAYING_TIMEOUT_MS = 3000L
    }
}
