package com.gaarj.iptvplayer.ui.viewholders

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gaarj.iptvplayer.R
import com.gaarj.iptvplayer.databinding.ItemChannelSettingsBinding
import com.gaarj.iptvplayer.domain.model.ChannelSettings

class ChannelSettingsViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private val binding = ItemChannelSettingsBinding.bind(view)

    fun render(itemId: Int, settings: ChannelSettings, onItemSelected: (Int) -> Unit){
        binding.tvChannelSettingsName.text = settings.getName()

        binding.tvChannelSettingsName.post{
            binding.tvChannelSettingsName.requestLayout()
        }

        itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                itemView.setBackgroundResource(R.drawable.bg_rounded_item_menu) // Highlight the focused item
                itemView.findViewById<TextView>(R.id.tvChannelSettingsName).setTextColor(Color.BLACK)
                binding.tvChannelSettingsName.post{
                    binding.tvChannelSettingsName.requestLayout()
                }
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT) // Reset when not focused
                itemView.findViewById<TextView>(R.id.tvChannelSettingsName).setTextColor(Color.WHITE)
            }
        }

        itemView.setOnClickListener {
            onItemSelected(itemId)
        }
    }
}