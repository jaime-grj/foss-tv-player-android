package com.gaarx.iptvplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.domain.model.ChannelItem
import com.gaarx.iptvplayer.ui.viewholders.ChannelListViewHolder

class ChannelListAdapter(private val currentCategoryId: Long, private val channelList: List<ChannelItem>, private val onChannelSelected: (ChannelItem) -> Unit) : RecyclerView.Adapter<ChannelListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ChannelListViewHolder(layoutInflater.inflate(R.layout.item_channel_list, parent, false))
    }

    override fun onBindViewHolder(holder: ChannelListViewHolder, position: Int) {
        val channel = channelList[position]
        holder.render(currentCategoryId, channel, onChannelSelected)
    }

    override fun getItemCount(): Int = channelList.size

    fun getItemAtPosition(position: Int): ChannelItem? {
        return if (position >= 0 && position < channelList.size) {
            channelList[position]
        } else {
            null // Or handle this case as needed
        }
    }
}