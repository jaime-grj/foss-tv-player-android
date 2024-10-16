package com.gaarj.iptvplayer.ui.adapters

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.domain.model.StreamSourceItem
import com.gaarj.iptvplayer.databinding.ItemChannelTrackSettingsBinding

class ChannelSourcesViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private val binding = ItemChannelTrackSettingsBinding.bind(view)

    fun render(source: StreamSourceItem, onItemSelected: (StreamSourceItem) -> Unit){

        if (source.isSelected) {
            binding.tvChannelSettingsTrackName.isChecked = true
        }

        if (source.index != -1) {
            binding.tvChannelSettingsTrackName.text = source.index.toString() + ": " + source.name
        }
        else{
            binding.tvChannelSettingsTrackName.text = source.name
        }

        binding.tvChannelSettingsTrackName.post{
            binding.tvChannelSettingsTrackName.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvChannelSettingsTrackName.setTextColor(Color.BLACK)
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu)
                binding.tvChannelSettingsTrackName.post{
                    binding.tvChannelSettingsTrackName.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                binding.tvChannelSettingsTrackName.setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onItemSelected(source)
        }
    }
}