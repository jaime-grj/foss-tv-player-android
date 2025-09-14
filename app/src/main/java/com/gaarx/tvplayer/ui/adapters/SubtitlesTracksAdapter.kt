package com.gaarx.tvplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.tvplayer.R
import com.gaarx.tvplayer.domain.model.SubtitlesTrack
import com.gaarx.tvplayer.ui.viewholders.SubtitlesTracksViewHolder

class SubtitlesTracksAdapter(private val subtitlesTrackList: List<SubtitlesTrack>, private val onItemSelected: (SubtitlesTrack) -> Unit) : RecyclerView.Adapter<SubtitlesTracksViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtitlesTracksViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return SubtitlesTracksViewHolder(layoutInflater.inflate(R.layout.item_channel_track_settings, parent, false))
    }

    override fun onBindViewHolder(holder: SubtitlesTracksViewHolder, position: Int) {
        val subtitlesTrack = subtitlesTrackList[position]
        holder.render(subtitlesTrack, onItemSelected)
    }

    override fun getItemCount(): Int = subtitlesTrackList.size

    fun getItemAtPosition(position: Int): SubtitlesTrack? {
        return if (position >= 0 && position < subtitlesTrackList.size) {
            subtitlesTrackList[position]
        } else {
            null // Or handle this case as needed
        }
    }
}