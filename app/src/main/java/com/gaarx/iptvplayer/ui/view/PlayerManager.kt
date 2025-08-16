package com.gaarx.iptvplayer.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.gaarx.iptvplayer.databinding.FragmentPlayerBinding
import com.gaarx.iptvplayer.domain.model.MediaInfo
import com.gaarx.iptvplayer.ui.viewmodel.PlayerViewModel
import com.gaarx.iptvplayer.core.MediaUtils
import com.google.common.util.concurrent.MoreExecutors
import androidx.media3.exoplayer.*
import androidx.media3.ui.SubtitleView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.common.*
import androidx.core.content.res.ResourcesCompat
import android.graphics.Color
import android.widget.FrameLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.CaptionStyleCompat
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.core.Constants.DEFAULT_REFRESH_RATE
import com.gaarx.iptvplayer.core.ytlivedashmanifestparser.LiveDashManifestParser
import com.gaarx.iptvplayer.data.services.ApiService
import com.gaarx.iptvplayer.domain.model.AudioTrack
import com.gaarx.iptvplayer.domain.model.DrmTypeItem
import com.gaarx.iptvplayer.domain.model.StreamSourceItem
import com.gaarx.iptvplayer.domain.model.SubtitlesTrack
import com.gaarx.iptvplayer.domain.model.VideoTrack
import com.gaarx.iptvplayer.ui.view.PlayerFragment.Companion.TAG
import com.gaarx.iptvplayer.ui.viewmodel.ChannelViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chromium.net.CronetEngine
import java.text.DecimalFormat
import java.util.Locale

@UnstableApi
class PlayerManager(
    private val context: Context,
    private val binding: FragmentPlayerBinding,
    private val playerViewModel: PlayerViewModel,
    private val channelViewModel: ChannelViewModel,
    private val mediaInfo: MediaInfo,
    private val timerManager: PlayerTimerManager,
    private val onIsPlayingChanged: (() -> Unit)? = null,
    private val onVideoFormatChanged: ((Float) -> Unit)? = null,
    private val onTryNextStreamSource: (() -> Unit)? = null
) {

    lateinit var player: ExoPlayer
    lateinit var trackSelector: DefaultTrackSelector

    private var currentAudioTrack: AudioTrack? = null
    private var currentVideoTrack: VideoTrack? = null
    private var currentSubtitlesTrack: SubtitlesTrack? = null

    private var apiService: ApiService = ApiService()

    val exoPlayer: ExoPlayer
        get() = player


    fun init(): ExoPlayer {
        binding.playerView.useController = false

        trackSelector = DefaultTrackSelector(context)
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

        val renderersFactory = DefaultRenderersFactory( context)
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

        val bandwidthMeter: DefaultBandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(50_000_000)
            .build()

        player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector).setRenderersFactory(
                renderersFactory
                    .setEnableDecoderFallback(true)
                    .setMediaCodecSelector(MediaCodecSelector.DEFAULT))
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .setDetachSurfaceTimeoutMs(4000)
            .build()

        binding.playerView.player = player
        setSubtitleStyle()
        attachListeners()

        return player
    }

    private fun setSubtitleStyle() {
        val subtitleView: SubtitleView? = binding.playerView.subtitleView

        val captionStyle = CaptionStyleCompat(
            Color.WHITE,
            Color.argb(192, 0, 0, 0),
            Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,  // Outline stroke type
            Color.BLACK,  // Edge color (Black for better contrast)
            ResourcesCompat.getFont(context, R.font.lato_bold)
        )
        subtitleView?.setStyle(captionStyle)

        subtitleView?.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.25f)
        (subtitleView?.layoutParams as? FrameLayout.LayoutParams)?.setMargins(30, 30, 30, 30)
    }

    private fun attachListeners() {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                timerManager.startHidePlayerTimer {
                    playerViewModel.hidePlayer()
                    timerManager.startLoadingIndicatorTimer {
                        if (playerViewModel.isAnimatedLoadingIconVisible.value == false) {
                            if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                            playerViewModel.showAnimatedLoadingIcon()
                        }
                    }
                }
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
                onTryNextStreamSource?.invoke()
            }

            override fun onRenderedFirstFrame() {
                if (playerViewModel.isErrorMessageVisible.value == true) playerViewModel.hideErrorMessage()
                playerViewModel.hideBottomErrorMessage()
                if (playerViewModel.isAnimatedLoadingIconVisible.value == true) playerViewModel.hideAnimatedLoadingIcon()
                timerManager.cancelLoadingIndicatorTimer()
                timerManager.cancelHidePlayerTimer()
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
                    val highestResolution = MediaUtils.getHighestResolution(tracks)
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
                    if (playerViewModel.isSourceLoading.value == true){
                        timerManager.cancelSourceLoadingTimer()
                        playerViewModel.setIsSourceLoading(false)
                    }
                    if (playerViewModel.isBuffering.value == true) timerManager.cancelBufferingTimer()
                    timerManager.cancelSourceLoadingTimer()
                    timerManager.cancelBufferingTimer()
                    timerManager.cancelLoadingIndicatorTimer()

                    playerViewModel.hideAnimatedLoadingIcon()
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
                    if (playerViewModel.isSourceLoading.value == true) timerManager.cancelSourceLoadingTimer()
                    if (playerViewModel.isBuffering.value == true) timerManager.cancelBufferingTimer()
                    timerManager.cancelCheckPlayingCorrectlyTimer()
                    if (playerViewModel.isMediaInfoVisible.value == true) playerViewModel.hideMediaInfo()
                    timerManager.startHidePlayerTimer{
                        playerViewModel.hidePlayer()
                        timerManager.startLoadingIndicatorTimer {
                            if (playerViewModel.isAnimatedLoadingIconVisible.value == false) {
                                if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                                playerViewModel.showAnimatedLoadingIcon()
                            }
                        }
                    }
                }
                else if (playbackState == Player.STATE_BUFFERING){
                    Log.i(TAG, "buffering")
                    if (playerViewModel.isPlayerVisible.value != true){
                        playerViewModel.showPlayer()
                    }
                    timerManager.cancelCheckPlayingCorrectlyTimer()
                    if (playerViewModel.isSourceLoading.value == false){
                        timerManager.startHidePlayerTimer {
                            playerViewModel.hidePlayer()
                            timerManager.startLoadingIndicatorTimer {
                                if (playerViewModel.isAnimatedLoadingIconVisible.value == false) {
                                    if (playerViewModel.isPlayerVisible.value == true) playerViewModel.hidePlayer()
                                    playerViewModel.showAnimatedLoadingIcon()
                                }
                            }
                        }
                        timerManager.startBufferingTimer {
                            playerViewModel.setIsBuffering(false)
                            val channel = playerViewModel.currentChannel.value
                            val source = playerViewModel.currentStreamSource.value
                            if (channel != null && source != null) {
                                onTryNextStreamSource?.invoke()
                            }
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (playerViewModel.isSourceLoading.value == true) timerManager.cancelSourceLoadingTimer()
                    if (playerViewModel.isBuffering.value == true) timerManager.cancelBufferingTimer()
                    if (playerViewModel.isChannelLoading.value == true) timerManager.cancelLoadingIndicatorTimer()
                    timerManager.cancelHidePlayerTimer()
                    timerManager.cancelSourceLoadingTimer()
                    timerManager.cancelBufferingTimer()

                    playerViewModel.hideAnimatedLoadingIcon()
                    playerViewModel.hideErrorMessage()
                    playerViewModel.hideBottomErrorMessage()
                    //playerViewModel.showPlayer()

                    onIsPlayingChanged?.invoke()

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
                    timerManager.startCheckPlayingCorrectlyTimer{
                        playerViewModel.updateSourcesTriedCount(0)
                        playerViewModel.updateTriesCountForEachSource(0)
                    }
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
                    playerViewModel.currentStreamSource.value?.refreshRate!!
                }
                else{
                    DEFAULT_REFRESH_RATE
                }
                onVideoFormatChanged?.invoke(frameRateSwitch)
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

    fun loadSubtitlesTrack(subtitlesTrack: SubtitlesTrack){
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

    fun loadAudioTrack(audioTrack: AudioTrack) {
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

    fun loadVideoTrack(videoTrack: VideoTrack) {
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

    suspend fun prepareMediaSource(streamSource: StreamSourceItem, headers: Map<String, String>, url: String) {
        currentAudioTrack = null
        currentVideoTrack = null
        currentSubtitlesTrack = null

        val cronetEngine = CronetEngine.Builder(context).build()
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
                    val apiResponse = apiService.getDrmKeys(streamSource)
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
        withContext(Dispatchers.Main) {
            player.setMediaSource(mediaSource)

            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverrides()
                .build()
            player.prepare()
        }
    }

    fun getAudioTrackLoader(): (AudioTrack) -> Unit = { track ->
        loadAudioTrack(track)
    }

    fun getSubtitlesTrackLoader(): (SubtitlesTrack) -> Unit = { track ->
        loadSubtitlesTrack(track)
    }

    fun getVideoTrackLoader(): (VideoTrack) -> Unit = { track ->
        loadVideoTrack(track/*, playerViewModel.isQualityForced.value == true*/)
    }
}
