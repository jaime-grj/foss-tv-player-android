package com.gaarj.iptvplayer.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.text.Spanned
import android.text.SpannedString
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.egeniq.androidtvprogramguide.ProgramGuideFragment
import com.gaarj.iptvplayer.R
import com.egeniq.androidtvprogramguide.entity.ProgramGuideChannel
import com.egeniq.androidtvprogramguide.entity.ProgramGuideSchedule
import com.gaarj.iptvplayer.ui.viewmodel.ChannelViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class EpgFragment: ProgramGuideFragment<EpgFragment.SimpleProgram>() {

    // Feel free to change configuration values like this:
    //
    // override val DISPLAY_CURRENT_TIME_INDICATOR = false
    // override val DISPLAY_SHOW_PROGRESS = false
    //override val DISPLAY_LOCALE: Locale = Locale("en", "US")
    override val CAN_FOCUS_CHANNEL = true
    override val SCROLL_SYNCING = true
    override val DISPLAY_TIMEZONE: ZoneId = ZoneOffset.systemDefault()

    companion object {
        private val TAG = EpgFragment::class.java.name
    }

    data class SimpleChannel(
        override val id: String,
        override val name: Spanned?,
        override val imageUrl: String?
    ) : ProgramGuideChannel

    // You can put your own data in the program class
    data class SimpleProgram(
        val id: String,
        val description: String,
        val metadata: String
    )

    private val channelViewModel: ChannelViewModel by viewModels()

    private lateinit var jobLoadEPG : Job

    override fun onScheduleClicked(programGuideSchedule: ProgramGuideSchedule<SimpleProgram>) {
        val innerSchedule = programGuideSchedule.program
        if (innerSchedule == null) {
            // If this happens, then our data source gives partial info
            Log.w(TAG, "Unable to open schedule!")
            return
        }
        if (programGuideSchedule.isCurrentProgram) {
            //Toast.makeText(context, "Open live player", Toast.LENGTH_LONG).show()
            val resultIntent = Intent()
            val channelId = programGuideSchedule.channelId
            Log.d(TAG, "Channel ID: $channelId")
            resultIntent.putExtra("channelId", channelId)
            activity?.setResult(Activity.RESULT_OK, resultIntent)
            activity?.onBackPressed()
        } else {
            Toast.makeText(context, "Open detail page", Toast.LENGTH_LONG).show()
        }
        // Example of how a program can be updated. You could also change the underlying program.
        updateProgram(programGuideSchedule.copy(displayTitle = programGuideSchedule.displayTitle + " [clicked]"))
    }

    override fun onScheduleSelected(programGuideSchedule: ProgramGuideSchedule<SimpleProgram>?) {
        val titleView = view?.findViewById<TextView>(R.id.programguide_detail_title)
        titleView?.text = programGuideSchedule?.displayTitle
        val metadataView = view?.findViewById<TextView>(R.id.programguide_detail_metadata)
        metadataView?.text = programGuideSchedule?.program?.metadata
        val descriptionView = view?.findViewById<TextView>(R.id.programguide_detail_description)
        descriptionView?.text = programGuideSchedule?.program?.description
        val imageView = view?.findViewById<ImageView>(R.id.programguide_detail_image) ?: return
        if (programGuideSchedule != null) {
            /*Glide.with(imageView)
                .load("https://picsum.photos/462/240?random=" + programGuideSchedule.displayTitle.hashCode())
                .centerCrop()
                .error(R.drawable.programguide_icon_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(withCrossFade())
                .into(imageView)*/
            Glide.with(imageView).clear(imageView)
        } else {
            Glide.with(imageView).clear(imageView)
        }
    }

    override fun onChannelSelected(channel: ProgramGuideChannel) {
        val titleView = view?.findViewById<TextView>(R.id.programguide_detail_title)
        titleView?.text = channel.name
        val metadataView = view?.findViewById<TextView>(R.id.programguide_detail_metadata)
        metadataView?.text = null
        val descriptionView = view?.findViewById<TextView>(R.id.programguide_detail_description)
        descriptionView?.text = null
        val imageView = view?.findViewById<ImageView>(R.id.programguide_detail_image) ?: return
        Glide.with(imageView).clear(imageView)
    }

    override fun onChannelClicked(channel: ProgramGuideChannel) {
        Toast.makeText(context, "Channel clicked: ${channel.name}", Toast.LENGTH_LONG).show()
    }

    override fun isTopMenuVisible(): Boolean {
        return false
    }

    @SuppressLint("CheckResult")
    override fun requestingProgramGuideFor(localDate: LocalDate) {
        // Set initial loading state
        setState(State.Loading)

        lifecycleScope.launch {
            val channelItems = channelViewModel.getSmChannelsWithSchedule()

            if (channelItems.isNotEmpty()) {
                val channelMap = ConcurrentHashMap<String, List<ProgramGuideSchedule<SimpleProgram>>>()
                val channels: MutableList<SimpleChannel> = mutableListOf()

                // Build channel list from database results
                for (channel in channelItems) {
                    channels += SimpleChannel(
                        channel.id.toString(),
                        if (channel.indexFavourite != null){
                            SpannedString(channel.indexFavourite.toString() + " " + channel.name)
                        } else
                            SpannedString(channel.indexGroup.toString() + " " + channel.name),
                        channel.logo
                    )
                }

                jobLoadEPG = lifecycleScope.launch {
                    // Populate schedules for each channel
                    channels.forEach { channel ->
                        val scheduleList = mutableListOf<ProgramGuideSchedule<SimpleProgram>>()

                        val programs = channelViewModel.getEPGProgramsForChannel(channel.id.toLong())
                        Log.d(TAG, "Found ${programs.size} programs for channel ${channel.id}.")

                        for (program in programs) {
                            val newStartTime = ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(program.startTime.time),
                                DISPLAY_TIMEZONE
                            )
                            val newEndTime = ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(program.stopTime.time),
                                DISPLAY_TIMEZONE
                            )
                            //println("ID ${program.id}, Title: " + program.title)
                            //println("newStartTime: $newStartTime, newEndTime: $newEndTime")
                            scheduleList.add(
                                createSchedule(
                                    program.title,
                                    newStartTime,
                                    newEndTime,
                                    program.description,
                                    channel.id.toLong()
                                )
                            )
                        }

                        channelMap[channel.id] = scheduleList
                    }

                    // Update UI with loaded data
                    withContext(Dispatchers.Main) {
                        if (channels.isEmpty() || channelMap.isEmpty()) {
                            setState(State.Error("No channels loaded."))
                        } else {
                            setData(channels, channelMap, localDate)
                            setState(State.Content)
                        }
                    }
                }
            }
        }
    }


    private fun createSchedule(
        scheduleName: String,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        description: String,
        channelId: Long
    ): ProgramGuideSchedule<SimpleProgram> {
        val id = Random.nextLong(100_000L)
        val metadata = DateTimeFormatter.ofPattern("'Starts at' HH:mm").format(startTime)
        return ProgramGuideSchedule.createScheduleWithProgram(
            id,
            startTime.toInstant(),
            endTime.toInstant(),
            true,
            scheduleName,
            SimpleProgram(
                id.toString(),
                description,
                metadata
            ),
            channelId
        )
    }

    private fun randomTimeBetween(min: ZonedDateTime, max: ZonedDateTime): ZonedDateTime {
        val randomEpoch = Random.nextLong(min.toEpochSecond(), max.toEpochSecond())
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(randomEpoch), ZoneOffset.systemDefault())
    }


    override fun requestRefresh() {
        // You can refresh other data here as well.
        requestingProgramGuideFor(currentDate)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        if (::jobLoadEPG.isInitialized && jobLoadEPG.isActive) {
            jobLoadEPG.cancel()
        }
    }

}
