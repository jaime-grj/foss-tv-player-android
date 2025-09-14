package com.gaarx.tvplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.tvplayer.R
import com.gaarx.tvplayer.domain.model.StreamSourceItem
import com.gaarx.tvplayer.ui.viewholders.ChannelSourcesViewHolder

class ChannelSourcesAdapter (private val sourcesList: List<StreamSourceItem>, private val onItemSelected: (StreamSourceItem) -> Unit) : RecyclerView.Adapter<ChannelSourcesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelSourcesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ChannelSourcesViewHolder(layoutInflater.inflate(R.layout.item_channel_track_settings, parent, false))
    }

    override fun onBindViewHolder(holder: ChannelSourcesViewHolder, position: Int) {
        val source = sourcesList[position]
        holder.render(source, onItemSelected)
    }

    override fun getItemCount(): Int = sourcesList.size

    fun getItemAtPosition(position: Int): StreamSourceItem? {
        return if (position >= 0 && position < sourcesList.size) {
            sourcesList[position]
        } else {
            null // Or handle this case as needed
        }
    }
}