package com.gaarx.iptvplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.iptvplayer.R
import com.gaarx.iptvplayer.domain.model.ChannelSettings
import com.gaarx.iptvplayer.ui.viewholders.ChannelSettingsViewHolder

class ChannelSettingsAdapter(private val settingsList: List<ChannelSettings>, private val onItemSelected: (Int) -> Unit) : RecyclerView.Adapter<ChannelSettingsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelSettingsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ChannelSettingsViewHolder(layoutInflater.inflate(R.layout.item_channel_settings, parent, false))
    }

    override fun onBindViewHolder(holder: ChannelSettingsViewHolder, position: Int) {
        val setting = settingsList[position]
        holder.render(position, setting, onItemSelected)
    }

    override fun getItemCount(): Int = settingsList.size
}