package com.gaarx.iptvplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.domain.model.AudioTrack
import com.gaarx.iptvplayer.ui.viewholders.AudioTracksViewHolder

class AudioTracksAdapter(private val audioTrackList: List<AudioTrack>, private val onItemSelected: (AudioTrack) -> Unit) : RecyclerView.Adapter<AudioTracksViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioTracksViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return AudioTracksViewHolder(layoutInflater.inflate(R.layout.item_channel_track_settings, parent, false))
    }

    override fun onBindViewHolder(holder: AudioTracksViewHolder, position: Int) {
        val audioTrack = audioTrackList[position]
        holder.render(audioTrack, onItemSelected)
    }

    override fun getItemCount(): Int = audioTrackList.size

    fun getItemAtPosition(position: Int): AudioTrack? {
        return if (position >= 0 && position < audioTrackList.size) {
            audioTrackList[position]
        } else {
            null // Or handle this case as needed
        }
    }
}