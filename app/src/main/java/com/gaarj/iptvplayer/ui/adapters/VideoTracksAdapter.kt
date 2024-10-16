package com.gaarj.iptvplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.domain.model.VideoTrack

class VideoTracksAdapter(private val videoTrackList: List<VideoTrack>, private val onItemSelected: (VideoTrack) -> Unit) : RecyclerView.Adapter<VideoTracksViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoTracksViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return VideoTracksViewHolder(layoutInflater.inflate(R.layout.item_channel_track_settings, parent, false))
    }

    override fun onBindViewHolder(holder: VideoTracksViewHolder, position: Int) {
        val videoTrack = videoTrackList[position]
        holder.render(videoTrack, onItemSelected)
    }

    override fun getItemCount(): Int = videoTrackList.size

    fun getItemAtPosition(position: Int): VideoTrack? {
        return if (position >= 0 && position < videoTrackList.size) {
            videoTrackList[position]
        } else {
            null // Or handle this case as needed
        }
    }
}