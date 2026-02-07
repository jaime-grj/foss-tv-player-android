package com.gaarx.tvplayer.ui.viewholders

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarx.tvplayer.R
import com.gaarx.tvplayer.domain.model.StreamSourceItem
import com.gaarx.tvplayer.databinding.ItemChannelTrackSettingsBinding

class ChannelSourcesViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private val binding = ItemChannelTrackSettingsBinding.bind(view)

    fun render(source: StreamSourceItem, onItemSelected: (StreamSourceItem) -> Unit){

        val sourceName = source.name.orEmpty()
        binding.rbChannelSettingsTrack.isChecked = source.isSelected

        if (source.index == -1) {
            binding.tvChannelSettingsTrackName.text = sourceName
            if (source.url.isNotBlank()) {
                binding.tvChannelSettingsTrackSubtitle.text = source.url
                binding.tvChannelSettingsTrackSubtitle.visibility = View.VISIBLE
            } else {
                binding.tvChannelSettingsTrackSubtitle.visibility = View.GONE
            }
        }
        else{
            if (sourceName.isBlank()) {
                binding.tvChannelSettingsTrackName.text = source.index.toString() + " - Sin nombre"
            }
            else{
                binding.tvChannelSettingsTrackName.text = source.index.toString() + " - " + sourceName
            }
            binding.tvChannelSettingsTrackSubtitle.text = source.url
            binding.tvChannelSettingsTrackSubtitle.visibility = View.VISIBLE
        }

        binding.tvChannelSettingsTrackName.post{
            binding.tvChannelSettingsTrackName.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvChannelSettingsTrackName.setTextColor(Color.BLACK)
                binding.tvChannelSettingsTrackSubtitle.setTextColor(Color.BLACK)
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu)
                binding.tvChannelSettingsTrackName.post{
                    binding.tvChannelSettingsTrackName.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                binding.tvChannelSettingsTrackName.setTextColor(Color.WHITE)
                binding.tvChannelSettingsTrackSubtitle.setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onItemSelected(source)
        }
    }
}